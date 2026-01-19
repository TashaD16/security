package com.example.gateway.security;

import com.example.common.security.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;

/**
 * Scanner that automatically discovers endpoints and their security annotations
 * to configure SecurityWebFilterChain without manual pathMatchers() configuration.
 * 
 * This scanner automatically finds all @RestController and @Controller beans,
 * extracts their request mappings, and maps them to authorization methods based
 * on security annotations (@RequiresReadDeclaration, @RequiresWriteDeclaration, etc.)
 * 
 * Improvements:
 * 1. Caching of scan results (scans once at startup)
 * 2. Support for Ant-patterns for path variables (prepared for future use)
 * 3. Optimized controller search (priority to ApplicationContext)
 * 4. Validation of duplicate paths
 * 5. Better handling of proxy classes (CGLIB and JDK dynamic proxies)
 * 6. Path normalization (removes double slashes, ensures proper format)
 * 7. Parallel processing for classpath scanning
 */
public class EndpointSecurityScanner {

    private static final Logger logger = LoggerFactory.getLogger(EndpointSecurityScanner.class);
    
    private final ApplicationContext applicationContext;
    private final CustomAuthorizationManager authorizationManager;
    // AntPathMatcher can be used for future improvements (e.g., pattern matching)
    @SuppressWarnings("unused")
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // Cache scan results
    private volatile EndpointSecurityScanResult cachedResult;
    private final Object scanLock = new Object();
    
    public EndpointSecurityScanner(ApplicationContext applicationContext, 
                                  CustomAuthorizationManager authorizationManager) {
        this.applicationContext = applicationContext;
        this.authorizationManager = authorizationManager;
    }
    
    /**
     * Initialization - scan at application startup
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing EndpointSecurityScanner - performing initial scan...");
        scanEndpoints(); // Pre-scan and cache
    }
    
    /**
     * Get scan results (from cache or perform scan)
     */
    public EndpointSecurityScanResult scanEndpoints() {
        if (cachedResult != null) {
            logger.debug("Returning cached scan result");
            return cachedResult;
        }
        
        synchronized (scanLock) {
            // Double-check locking
            if (cachedResult != null) {
                return cachedResult;
            }
            
            logger.info("Performing endpoint security scan...");
            cachedResult = performScan();
            logger.info("Scan complete. Cached {} secured paths and {} permitAll paths", 
                    cachedResult.getSecuredPaths().size(), 
                    cachedResult.getPermitAllPaths().size());
            
            return cachedResult;
        }
    }
    
    /**
     * Clear cache (for rescanning, e.g., during hot reload)
     */
    public void clearCache() {
        synchronized (scanLock) {
            cachedResult = null;
            logger.info("Endpoint security scan cache cleared");
        }
    }
    
    /**
     * Main scan method
     */
    private EndpointSecurityScanResult performScan() {
        Map<String, ReactiveAuthorizationManager<AuthorizationContext>> securityMap = new LinkedHashMap<>();
        Set<String> permitAllPaths = new LinkedHashSet<>();
        Set<String> processedPaths = new HashSet<>(); // For duplicate validation
        
        try {
            Map<String, Object> controllers = findControllers();
            logger.info("Found {} controllers to scan", controllers.size());
            
            for (Map.Entry<String, Object> entry : controllers.entrySet()) {
                Object controller = entry.getValue();
                Class<?> controllerClass = getControllerClass(controller);
                
                logger.debug("Processing controller: {}", controllerClass.getName());
                
                String basePath = extractBasePath(controllerClass);
                Method[] methods = getControllerMethods(controllerClass);
                
                for (Method method : methods) {
                    if (!isEndpointMethod(method)) {
                        continue;
                    }
                    
                    List<String> paths = extractPaths(method, basePath);
                    
                    for (String path : paths) {
                        // Normalize path (remove double slashes, add leading slash)
                        String normalizedPath = normalizePath(path);
                        
                        // Validate duplicates (allow different HTTP methods on same path)
                        if (processedPaths.contains(normalizedPath)) {
                            logger.warn("Duplicate path detected: {} - latest configuration will be used", normalizedPath);
                        }
                        processedPaths.add(normalizedPath);
                        
                        // Determine security rules
                        if (AnnotatedElementUtils.hasAnnotation(method, PermitAll.class)) {
                            permitAllPaths.add(normalizedPath);
                            logger.debug("PermitAll configured for: {} ({}.{})", 
                                    normalizedPath, controllerClass.getSimpleName(), method.getName());
                        } else {
                            ReactiveAuthorizationManager<AuthorizationContext> authManager = 
                                extractAuthorizationMethod(method);
                            
                            if (authManager != null) {
                                // Use LinkedHashMap to preserve order
                                securityMap.put(normalizedPath, authManager);
                                logger.debug("Security configured for: {} ({}.{})", 
                                        normalizedPath, controllerClass.getSimpleName(), method.getName());
                            } else {
                                logger.warn("No security annotation on method {}.{} - will require authentication only", 
                                        controllerClass.getSimpleName(), method.getName());
                            }
                        }
                    }
                }
            }
            
            logger.info("Scan completed: {} secured paths, {} permitAll paths", 
                    securityMap.size(), permitAllPaths.size());
                    
        } catch (Exception e) {
            logger.error("Error during endpoint scan", e);
            // Return empty result instead of failing
            return new EndpointSecurityScanResult(new HashMap<>(), new HashSet<>());
        }
        
        return new EndpointSecurityScanResult(securityMap, permitAllPaths);
    }
    
    /**
     * Find controllers with ApplicationContext priority
     */
    private Map<String, Object> findControllers() {
        Map<String, Object> controllers = new LinkedHashMap<>();
        
        // Step 1: Try to get from ApplicationContext (fastest way)
        try {
            Map<String, Object> contextControllers = new HashMap<>();
            contextControllers.putAll(applicationContext.getBeansWithAnnotation(Controller.class));
            contextControllers.putAll(applicationContext.getBeansWithAnnotation(RestController.class));
            
            if (!contextControllers.isEmpty()) {
                controllers.putAll(contextControllers);
                logger.info("Found {} controllers in ApplicationContext", controllers.size());
                return controllers;
            }
        } catch (Exception e) {
            logger.debug("Could not get controllers from ApplicationContext: {}", e.getMessage());
        }
        
        // Step 2: Fallback to classpath scan
        logger.info("No controllers in ApplicationContext, scanning classpath...");
        controllers = scanClasspathForControllers();
        
        return controllers;
    }
    
    /**
     * Optimized classpath scanning
     */
    private Map<String, Object> scanClasspathForControllers() {
        Map<String, Object> controllers = new LinkedHashMap<>();
        
        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
            
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + 
                    "com/example/**/*.class";
            
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            logger.debug("Scanning {} class files", resources.length);
            
            // Use Stream API for parallel processing
            List<Class<?>> controllerClasses = Arrays.stream(resources)
                    .parallel()
                    .filter(Resource::isReadable)
                    .map(resource -> {
                        try {
                            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            String className = metadataReader.getClassMetadata().getClassName();
                            
                            // Check annotations without loading class
                            boolean isController = metadataReader.getAnnotationMetadata().hasAnnotation(
                                    RestController.class.getName()) ||
                                metadataReader.getAnnotationMetadata().hasAnnotation(
                                    Controller.class.getName());
                            
                            if (isController) {
                                try {
                                    return Class.forName(className);
                                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                                    logger.debug("Could not load controller class: {} - {}", 
                                            className, e.getMessage());
                                    return null;
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            logger.trace("Error processing resource {}: {}", 
                                    resource.getFilename(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Convert to Map
            for (Class<?> clazz : controllerClasses) {
                controllers.put(clazz.getSimpleName(), clazz);
                logger.debug("Found controller: {}", clazz.getName());
            }
            
            logger.info("Classpath scan found {} controllers", controllers.size());
            
        } catch (Exception e) {
            logger.error("Error scanning classpath for controllers", e);
        }
        
        return controllers;
    }
    
    /**
     * Get controller class (handle proxies)
     */
    private Class<?> getControllerClass(Object controller) {
        if (controller instanceof Class) {
            return (Class<?>) controller;
        }
        
        Class<?> clazz = controller.getClass();
        
        // Handle CGLIB proxies ($$CGLIB)
        while (clazz.getName().contains("$$")) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null || superClass == Object.class) {
                break;
            }
            clazz = superClass;
        }
        
        // Handle JDK dynamic proxies
        if (clazz.getInterfaces().length > 0 && 
            clazz.getSimpleName().startsWith("$Proxy")) {
            for (Class<?> iface : clazz.getInterfaces()) {
                if (iface.getAnnotation(RestController.class) != null ||
                    iface.getAnnotation(Controller.class) != null) {
                    return iface;
                }
            }
        }
        
        return clazz;
    }
    
    /**
     * Get controller methods (only public, not from Object)
     */
    private Method[] getControllerMethods(Class<?> controllerClass) {
        return Arrays.stream(controllerClass.getMethods())
                .filter(m -> !m.isSynthetic())
                .filter(m -> !m.isBridge())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .toArray(Method[]::new);
    }
    
    /**
     * Check if method is an HTTP endpoint
     */
    private boolean isEndpointMethod(Method method) {
        return hasHttpMapping(method);
    }
    
    /**
     * Extract base path from controller class
     */
    private String extractBasePath(Class<?> controllerClass) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(
                controllerClass, RequestMapping.class);
        
        if (classMapping != null && classMapping.value().length > 0) {
            return normalizePath(classMapping.value()[0]);
        }
        
        return "";
    }
    
    /**
     * Extract paths from method
     */
    private List<String> extractPaths(Method method, String basePath) {
        List<String> paths = new ArrayList<>();
        
        // Check all types of HTTP mappings
        extractPathsFromAnnotation(method, GetMapping.class, basePath, paths);
        extractPathsFromAnnotation(method, PostMapping.class, basePath, paths);
        extractPathsFromAnnotation(method, PutMapping.class, basePath, paths);
        extractPathsFromAnnotation(method, DeleteMapping.class, basePath, paths);
        extractPathsFromAnnotation(method, PatchMapping.class, basePath, paths);
        
        // Fallback to @RequestMapping
        if (paths.isEmpty()) {
            extractPathsFromAnnotation(method, RequestMapping.class, basePath, paths);
        }
        
        return paths;
    }
    
    /**
     * Universal method for extracting paths from annotation
     */
    private <T extends Annotation> void extractPathsFromAnnotation(
            Method method, 
            Class<T> annotationType, 
            String basePath, 
            List<String> paths) {
        
        T mapping = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (mapping != null) {
            try {
                Method valueMethod = annotationType.getMethod("value");
                String[] methodPaths = (String[]) valueMethod.invoke(mapping);
                
                if (methodPaths != null && methodPaths.length > 0) {
                    for (String methodPath : methodPaths) {
                        paths.add(combinePaths(basePath, methodPath));
                    }
                } else {
                    paths.add(basePath);
                }
            } catch (Exception e) {
                logger.warn("Error extracting paths from {} annotation: {}", 
                        annotationType.getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * Combine base path and method path
     */
    private String combinePaths(String basePath, String methodPath) {
        String combined = basePath + methodPath;
        return normalizePath(combined);
    }
    
    /**
     * Normalize path (removes double slashes, adds leading slash)
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Remove double slashes
        path = path.replaceAll("//+", "/");
        
        // Remove trailing slash (except root path)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        // Add leading slash if missing
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return path;
    }
    
    /**
     * Check if method has HTTP mapping
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
     * Extract authorization method from annotations
     */
    private ReactiveAuthorizationManager<AuthorizationContext> extractAuthorizationMethod(Method method) {
        // Use Map for cleaner code
        Map<Class<? extends Annotation>, Supplier<ReactiveAuthorizationManager<AuthorizationContext>>> annotationMap = 
            new HashMap<>();
        annotationMap.put(RequiresReadDeclaration.class, () -> authorizationManager::checkReadDeclaration);
        annotationMap.put(RequiresWriteDeclaration.class, () -> authorizationManager::checkWriteDeclaration);
        annotationMap.put(RequiresApproveDeclaration.class, () -> authorizationManager::checkApproveDeclaration);
        annotationMap.put(RequiresReadWare.class, () -> authorizationManager::checkReadWare);
        annotationMap.put(RequiresWriteWare.class, () -> authorizationManager::checkWriteWare);
        annotationMap.put(RequiresWareInventory.class, () -> authorizationManager::checkWareInventory);
        annotationMap.put(RequiresGeneralAccess.class, () -> authorizationManager::checkGeneralAccess);
        
        for (Map.Entry<Class<? extends Annotation>, Supplier<ReactiveAuthorizationManager<AuthorizationContext>>> entry : annotationMap.entrySet()) {
            if (AnnotatedElementUtils.hasAnnotation(method, entry.getKey())) {
                return entry.getValue().get();
            }
        }
        
        return null;
    }
}
