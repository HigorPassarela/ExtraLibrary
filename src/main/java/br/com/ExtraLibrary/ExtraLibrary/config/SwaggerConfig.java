package br.com.ExtraLibrary.ExtraLibrary.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Extra Library API")
                        .description("API para gerenciamento de livraria com autenticação JWT e senhas criptografadas")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                // ← ADICIONADO: Servers para forçar HTTPS em produção
                .servers(Arrays.asList(
                        new Server()
                                .url("https://extralibrary-production.up.railway.app")
                                .description("Production Server"),
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server")
                ));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Insira o token JWT no formato: Bearer {token}");
    }
}
