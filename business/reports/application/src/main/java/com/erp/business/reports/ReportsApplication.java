package com.erp.business.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.erp.business.reports",
        "com.erp.platform.shared",
        "com.erp.platform.business"
})
@Import(com.erp.platform.common.openapi.OpenApiConfig.class)
public class ReportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportsApplication.class, args);
    }
}

