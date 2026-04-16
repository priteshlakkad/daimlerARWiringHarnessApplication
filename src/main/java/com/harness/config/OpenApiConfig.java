package com.harness.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Daimler AR Wiring Harness API", version = "1.0.0", description = "Comprehensive API documentation for the AR Wiring Harness application. "
        + "Supports file management for GLTF models, truck icons, images, and various harness-related documents (PDF, Video, JSON, Bundle).", contact = @io.swagger.v3.oas.annotations.info.Contact(name = "Support Team", email = "support@example.com"), license = @io.swagger.v3.oas.annotations.info.License(name = "Proprietary", url = "https://example.com/license")), servers = {
                @Server(url = "/", description = "Default Server")
        })
public class OpenApiConfig {
}