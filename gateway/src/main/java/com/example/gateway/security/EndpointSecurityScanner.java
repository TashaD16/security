package com.example.gateway.security;

import com.example.common.security.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.lang.reflect.Method;
import java.util.*;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;

/**
 * Scanner that automatically discovers endpoints and their security annotations
 * to configure SecurityWebFilterChain without manual pathMatchers() configuration.
 * 
 * This scanner automatically finds all @RestController and @Controller beans,
 * extracts their request mappings, and maps them to authorization methods based
 * on security annotations (@RequiresReadDeclaration, @RequiresWriteDeclaration, etc.)
 */
public class EndpointSecurityScanner {

    private static final Logger logger = LoggerFactory.getLogger(EndpointSecurityScanner.class);
    
    private final ApplicationContext applicationContext;
    private final CustomAuthorizationManager authorizationManager;
    
    public EndpointSecurityScanner(ApplicationContext applicationContext, 
                                  CustomAuthorizationManager authorizationManager) {
        this.applicationContext = applicationContext;
        this.authorizationManager = authorizationManager;
    }
    
    /**
     * Scans all controllers and builds security configuration
     * @return EndpointSecurityScanResult with secured paths and permitAll paths
     */
    public EndpointSecurityScanResult scanEndpoints() {
        Map<String, ReactiveAuthorizationManager<AuthorizationContext>> securityMap = new HashMap<>();
        Set<String> permitAllPaths = new HashSet<>();
        
        try {
            // First, try to get controllers from ApplicationContext (if they are Spring beans)
            Map<String, Object> controllers = new HashMap<>();
            try {
                controllers.putAll(applicationContext.getBeansWithAnnotation(Controller.class));
                controllers.putAll(applicationContext.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController.class));
            } catch (Exception e) {
                logger.debug("Could not get controllers from ApplicationContext: {}", e.getMessage());
            }
            
            // If no controllers found in ApplicationContext, scan classpath
            if (controllers.isEmpty()) {
                logger.info("No controllers found in ApplicationContext, scanning classpath...");
                controllers = scanClasspathForControllers();
            }
            
            logger.info("Scanning {} controllers for security annotations", controllers.size());
            
            for (Map.Entry<String, Object> entry : controllers.entrySet()) {
                Object controller = entry.getValue();
                Class<?> controllerClass = getControllerClass(controller);
                
                logger.debug("Processing controller: {}", controllerClass.getName());
                
                // Get base path from @RequestMapping on class
                String basePath = extractBasePath(controllerClass);
                logger.debug("Base path for controller {}: {}", controllerClass.getSimpleName(), basePath);
                
                // Scan all methods (both declared and inherited public methods)
                Method[] methods = controllerClass.getMethods();
                logger.debug("Found {} methods in controller {}", methods.length, controllerClass.getSimpleName());
                
                for (Method method : methods) {
                    // Skip synthetic methods, bridge methods, and Object methods
                    if (method.isSynthetic() || method.isBridge() || 
                        method.getDeclaringClass() == Object.class) {
                        continue;
                    }
                    
                    // Only process methods with HTTP mapping annotations
                    if (!hasHttpMapping(method)) {
                        logger.debug("Skipping method {} - no HTTP mapping annotation", method.getName());
                        continue;
                    }
                    
                    List<String> paths = extractPaths(method, basePath);
                    logger.debug("Method {} extracted {} paths: {}", method.getName(), paths.size(), paths);
                    
                    if (!paths.isEmpty()) {
                        // Check for @PermitAll first
                        if (AnnotatedElementUtils.hasAnnotation(method, PermitAll.class)) {
                            for (String path : paths) {
                                permitAllPaths.add(path);
                                logger.info("Auto-configured permitAll for path: {} using annotation from method: {}.{}", 
                                        path, controllerClass.getSimpleName(), method.getName());
                            }
                        } else {
                            // Check for other security annotations
                            ReactiveAuthorizationManager<AuthorizationContext> authManager = 
                                extractAuthorizationMethod(method);
                            
                            if (authManager != null) {
                                for (String path : paths) {
                                    securityMap.put(path, authManager);
                                    logger.info("Auto-configured security for path: {} using annotation from method: {}.{}", 
                                            path, controllerClass.getSimpleName(), method.getName());
                                }
                            } else {
                                logger.warn("No security annotation found on method {} in controller {} - endpoint will require authentication only", 
                                        method.getName(), controllerClass.getSimpleName());
                            }
                        }
                    }
                }
            }
            
            logger.info("Auto-configured security for {} endpoint paths, {} permitAll paths", 
                    securityMap.size(), permitAllPaths.size());
        } catch (Exception e) {
            logger.error("Error scanning endpoints", e);
        }
        
        return new EndpointSecurityScanResult(securityMap, permitAllPaths);
    }
    
    /**
     * Scan classpath for controller classes automatically in all modules
     * Uses ResourcePatternResolver to scan all jar files and classes in classpath
     * This works even if modules are not direct dependencies (scans all jars in classpath)
     */
    private Map<String, Object> scanClasspathForControllers() {
        Map<String, Object> controllers = new HashMap<>();
        
        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
            
            // Scan all classes in com.example package and subpackages from all jars/classes in classpath
            // CLASSPATH_ALL_URL_PREFIX ("classpath*:") searches in all jar files, not just current module
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    "com/example/**/*.class";
            
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            
            logger.debug("Scanning {} class files for controllers", resources.length);
            
            for (Resource resource : resources) {
                try {
                    if (!resource.isReadable()) {
                        continue;
                    }
                    
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    
                    // Check if class has @RestController or @Controller annotation
                    if (metadataReader.getAnnotationMetadata().hasAnnotation(
                            org.springframework.web.bind.annotation.RestController.class.getName()) ||
                        metadataReader.getAnnotationMetadata().hasAnnotation(
                            Controller.class.getName())) {
                        
                        try {
                            Class<?> clazz = Class.forName(className);
                            controllers.put(clazz.getSimpleName(), clazz);
                            logger.debug("Found controller class: {}", className);
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            logger.warn("Could not load controller class: {} - {}", className, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error processing resource {}: {}", resource.getFilename(), e.getMessage());
                }
            }
            
            logger.info("Found {} controllers in classpath scan", controllers.size());
        } catch (Exception e) {
            logger.error("Error scanning classpath for controllers", e);
        }
        
        return controllers;
    }
    
    /**
     * Get actual controller class (handle Spring proxies and Class objects)
     */
    private Class<?> getControllerClass(Object controller) {
        Class<?> clazz;
        if (controller instanceof Class) {
            clazz = (Class<?>) controller;
        } else {
            clazz = controller.getClass();
            // Handle CGLIB proxies
            while (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }
        }
        return clazz;
    }
    
    /**
     * Extract base path from controller class
     */
    private String extractBasePath(Class<?> controllerClass) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controllerClass, RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            return classMapping.value()[0];
        }
        return "";
    }
    
    /**
     * Extract all possible paths from method annotations
     */
    private List<String> extractPaths(Method method, String basePath) {
        List<String> paths = new ArrayList<>();
        
        // Check for @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @PatchMapping
        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (getMapping != null) {
            paths.addAll(buildPaths(basePath, getMapping.value()));
        }
        
        PostMapping postMapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (postMapping != null) {
            paths.addAll(buildPaths(basePath, postMapping.value()));
        }
        
        PutMapping putMapping = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (putMapping != null) {
            paths.addAll(buildPaths(basePath, putMapping.value()));
        }
        
        DeleteMapping deleteMapping = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) {
            paths.addAll(buildPaths(basePath, deleteMapping.value()));
        }
        
        PatchMapping patchMapping = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patchMapping != null) {
            paths.addAll(buildPaths(basePath, patchMapping.value()));
        }
        
        // Fallback to @RequestMapping
        if (paths.isEmpty()) {
            RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (requestMapping != null) {
                paths.addAll(buildPaths(basePath, requestMapping.value()));
            }
        }
        
        return paths;
    }
    
    /**
     * Build full paths from base path and method paths
     */
    private List<String> buildPaths(String basePath, String[] methodPaths) {
        List<String> fullPaths = new ArrayList<>();
        
        if (methodPaths.length == 0) {
            methodPaths = new String[]{""};
        }
        
        for (String methodPath : methodPaths) {
            String fullPath = basePath + methodPath;
            if (!fullPath.startsWith("/")) {
                fullPath = "/" + fullPath;
            }
            // Normalize path (remove double slashes)
            fullPath = fullPath.replaceAll("//+", "/");
            fullPaths.add(fullPath);
        }
        
        return fullPaths;
    }
    
    /**
     * Check if method has any HTTP mapping annotation
     */
    private boolean hasHttpMapping(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, GetMapping.class) ||
               AnnotatedElementUtils.hasAnnotation(method, PostMapping.class) ||
               AnnotatedElementUtils.hasAnnotation(method, PutMapping.class) ||
               AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class) ||
               AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class) ||
               AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }
    
    /**
     * Extract authorization manager from method annotations
     * Uses AnnotatedElementUtils for more reliable annotation detection
     */
    private ReactiveAuthorizationManager<AuthorizationContext> extractAuthorizationMethod(Method method) {
        // Check for security annotations using AnnotatedElementUtils for better proxy support
        // Methods now accept Mono<Authentication> directly, so we can use method references
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresReadDeclaration.class)) {
            return authorizationManager::checkReadDeclaration;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresWriteDeclaration.class)) {
            return authorizationManager::checkWriteDeclaration;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresApproveDeclaration.class)) {
            return authorizationManager::checkApproveDeclaration;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresReadWare.class)) {
            return authorizationManager::checkReadWare;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresWriteWare.class)) {
            return authorizationManager::checkWriteWare;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresWareInventory.class)) {
            return authorizationManager::checkWareInventory;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, RequiresGeneralAccess.class)) {
            return authorizationManager::checkGeneralAccess;
        }
        
        return null;
    }
}
