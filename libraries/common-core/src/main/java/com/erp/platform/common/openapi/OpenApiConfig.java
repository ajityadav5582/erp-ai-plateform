package com.erp.platform.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared OpenAPI/Swagger configuration for all ERP services.
 * Each service inherits this bean via common-core and can override
 * the title/description by defining its own OpenAPI bean.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI erpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP AI Platform API")
                        .description("REST API for the ERP AI Platform microservices")
                        .version("1.0.0"));
    }
}
