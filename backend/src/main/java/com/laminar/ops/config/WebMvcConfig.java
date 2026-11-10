package com.laminar.ops.config;

import com.laminar.ops.security.AuthInterceptor;
import com.laminar.ops.security.OperLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final OperLogInterceptor operLogInterceptor;

    @Value("${laminar.upload.path}")
    private String uploadPath;

    @Value("${laminar.web.admin-dist:D:/ideaWork/laminar-ops/web-admin/dist}")
    private String adminDist;

    @Value("${laminar.web.portal-dist:D:/ideaWork/laminar-ops/web-admin/dist-portal}")
    private String portalDist;

    @Value("${laminar.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${laminar.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/captcha",
                        "/api/public/**",
                        "/api/open/**",
                        "/error"
                );
        registry.addInterceptor(operLogInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**", "/error");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = java.util.stream.Stream.concat(
                        java.util.Arrays.stream(allowedOrigins.split(",")),
                        java.util.Arrays.stream(allowedOriginPatterns.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + (uploadPath.endsWith("/") ? uploadPath : uploadPath + "/"));
        registry.addResourceHandler("/app/**")
                .addResourceLocations("file:" + (adminDist.endsWith("/") ? adminDist : adminDist + "/"))
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler("/portal/**")
                .addResourceLocations("file:" + (portalDist.endsWith("/") ? portalDist : portalDist + "/"))
                .setCacheControl(CacheControl.noCache());
    }
}
