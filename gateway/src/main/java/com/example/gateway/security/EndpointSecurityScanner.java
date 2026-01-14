package com.example.gateway.security;

import com.example.common.security.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

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
                
                // Get base path from @RequestMapping on class
                String basePath = extractBasePath(controllerClass);
                
                // Scan all methods
                for (Method method : controllerClass.getDeclaredMethods()) {
                    List<String> paths = extractPaths(method, basePath);
                    
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
     * Scan classpath for controller classes
     */
    private Map<String, Object> scanClasspathForControllers() {
        Map<String, Object> controllers = new HashMap<>();
        
        try {
            ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
            
            // Add filters for @RestController and @Controller
            scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.web.bind.annotation.RestController.class));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
            
            // Scan packages from moduleA and moduleB
            Set<org.springframework.beans.factory.config.BeanDefinition> candidates = new HashSet<>();
            candidates.addAll(scanner.findCandidateComponents("com.example.modulea.controller"));
            candidates.addAll(scanner.findCandidateComponents("com.example.moduleb.controller"));
            
            for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
                try {
                    Class<?> clazz = Class.forName(candidate.getBeanClassName());
                    // Create a placeholder object for scanning (we only need the class)
                    controllers.put(clazz.getSimpleName(), clazz);
                } catch (ClassNotFoundException e) {
                    logger.warn("Could not load class: {}", candidate.getBeanClassName());
                }
            }
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
