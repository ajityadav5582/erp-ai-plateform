package com.erp.platform.testing.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Base annotation for integration tests.
 *
 * <p>This annotation combines common configurations needed
 * for integration tests in the ERP AI Platform.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @BaseIntegrationTest
 * class MyServiceIntegrationTest {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface BaseIntegrationTest {

    /**
     * Optional Spring Boot properties to add to the test.
     *
     * @return the properties
     */
    String[] properties() default {};

    /**
     * Whether to use random port.
     *
     * @return true to use random port
     */
    boolean randomPort() default true;
}
