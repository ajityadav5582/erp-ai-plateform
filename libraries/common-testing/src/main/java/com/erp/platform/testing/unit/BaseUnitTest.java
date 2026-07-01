package com.erp.platform.testing.unit;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

/**
 * Base annotation for unit tests.
 *
 * <p>This annotation combines common configurations needed
 * for unit tests in the ERP AI Platform.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @BaseUnitTest
 * class MyServiceUnitTest {
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
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface BaseUnitTest {
}
