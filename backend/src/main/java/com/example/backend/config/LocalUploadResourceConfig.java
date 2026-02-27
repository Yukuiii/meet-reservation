package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地上传文件静态资源映射配置。
 */
@Configuration
public class LocalUploadResourceConfig implements WebMvcConfigurer {

    /**
     * 本地上传根目录。
     */
    @Value("${app.upload.base-dir:${user.dir}/uploads}")
    private String uploadBaseDir;

    /**
     * 注册本地上传目录的静态资源访问映射。
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        String resourceLocation = uploadDir.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
