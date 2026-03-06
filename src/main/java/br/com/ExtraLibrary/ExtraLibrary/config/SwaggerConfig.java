package br.com.ExtraLibrary.ExtraLibrary.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

    private final Environment environment;

    @Value("${server.port}")
    private String serverPort;

    public SwaggerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String currentProfile = getCurrentProfile();

        return new OpenAPI()
                .info(new Info()
                        .title("Extra Library API")
                        .description(getApiDescription(currentProfile))
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                .servers(getServers());
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Insira o token JWT no formato: Bearer {token}");
    }

    private String getApiDescription(String profile) {
        switch (profile) {
            case "production":
                return "🚀 API para gerenciamento de livraria - PRODUÇÃO";
            case "homolog":
                return "🧪 API para gerenciamento de livraria - HOMOLOGAÇÃO";
            case "development":
                return "🔧 API para gerenciamento de livraria - DESENVOLVIMENTO";
            default:
                return "API para gerenciamento de livraria";
        }
    }

    private List<Server> getServers() {
        String activeProfile = getCurrentProfile();

        switch (activeProfile) {
            case "production":
                return Arrays.asList(
                        new Server()
                                .url("https://extralibrary-prd.up.railway.app")
                                .description("🚀 Production Server")
                );

            case "homolog":
                return Arrays.asList(
                        new Server()
                                .url("https://extralibrary-hom.up.railway.app")
                                .description("🧪 Homolog Server"),
                        new Server()
                                .url("https://extralibrary-dev.up.railway.app")
                                .description("🔧 Development Server")
                );

            case "development":
            default:
                return Arrays.asList(
                        new Server()
                                .url("https://extralibrary-dev.up.railway.app")
                                .description("🔧 Development Server"),
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("💻 Local Development")
                );
        }
    }

    private String getCurrentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "development";
    }
}
