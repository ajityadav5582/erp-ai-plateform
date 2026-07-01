package com.erp.platform.common.dto;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generic result wrapper for operation outcomes.
 *
 * <p>Provides a type-safe way to represent success or failure outcomes
 * without using exceptions for control flow. This is inspired by
 * functional programming patterns and provides a cleaner alternative
 * to returning null or throwing exceptions for expected failure cases.
 *
 * <p>This class is sealed to prevent external inheritance and ensure
 * all possible outcomes are handled.
 *
 * @param <T> the type of the success value
 * @since 1.0.0
 */
public sealed interface Result<T> extends Serializable {

    /**
     * Creates a successful result.
     *
     * @param value the success value
     * @param <T> the type of the value
     * @return a successful Result
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result.
     *
     * @param error the error details
     * @param <T> the type of the success value
     * @return a failed Result
     */
    static <T> Result<T> failure(ErrorDetail error) {
        return new Failure<>(error);
    }

    /**
     * Creates a failed result from an error code and message.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param <T> the type of the success value
     * @return a failed Result
     */
    static <T> Result<T> failure(String errorCode, String message) {
        return new Failure<>(new ErrorDetail(errorCode, message));
    }

    /**
     * Checks if the result is successful.
     *
     * @return true if successful, false otherwise
     */
    boolean isSuccess();

    /**
     * Checks if the result is a failure.
     *
     * @return true if failed, false otherwise
     */
    boolean isFailure();

    /**
     * Returns the success value.
     *
     * @return the success value
     * @throws IllegalStateException if the result is a failure
     */
    T getValue();

    /**
     * Returns the error details.
     *
     * @return the error details, or null if successful
     */
    ErrorDetail getError();

    /**
     * Maps the success value to a new type.
     *
     * @param mapper the mapping function
     * @param <R> the type of the mapped value
     * @return a new Result with the mapped value
     */
    <R> Result<R> map(Function<? super T, ? extends R> mapper);

    /**
     * Flat maps the success value to a new Result.
     *
     * @param mapper the mapping function
     * @param <R> the type of the mapped value
     * @return a new Result with the mapped value
     */
    <R> Result<R> flatMap(Function<? super T, ? extends Result<? extends R>> mapper);

    /**
     * Handles the result with separate functions for success and failure.
     *
     * @param onSuccess the success handler
     * @param onFailure the failure handler
     * @param <R> the return type
     * @return the result of the appropriate handler
     */
    <R> R handle(Function<? super T, ? extends R> onSuccess, Function<? super ErrorDetail, ? extends R> onFailure);

    /**
     * Executes the supplier and wraps the result.
     *
     * @param supplier the supplier to execute
     * @param <T> the type of the value
     * @return a Result wrapping the supplier outcome
     */
    static <T> Result<T> from(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(ErrorDetail.of(e));
        }
    }

    /**
     * Successful result implementation.
     *
     * @param <T> the type of the value
     */
    record Success<T>(T value) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public ErrorDetail getError() {
            return null;
        }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return success(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<? super T, ? extends Result<? extends R>> mapper) {
            return (Result<R>) mapper.apply(value);
        }

        @Override
        public <R> R handle(Function<? super T, ? extends R> onSuccess,
                            Function<? super ErrorDetail, ? extends R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    /**
     * Failed result implementation.
     *
     * @param <T> the type of the success value
     */
    record Failure<T>(ErrorDetail error) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T getValue() {
            throw new IllegalStateException("Cannot get value from failed result: " + error);
        }

        @Override
        public ErrorDetail getError() {
            return error;
        }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return failure(error);
        }

        @Override
        public <R> Result<R> flatMap(Function<? super T, ? extends Result<? extends R>> mapper) {
            return failure(error);
        }

        @Override
        public <R> R handle(Function<? super T, ? extends R> onSuccess,
                            Function<? super ErrorDetail, ? extends R> onFailure) {
            return onFailure.apply(error);
        }
    }

    /**
     * Error detail for failed results.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param details additional error details
     */
    record ErrorDetail(
        String errorCode,
        String message,
        java.util.Map<String, Object> details
    ) implements Serializable {

        /**
         * Creates error detail with minimal information.
         */
        public ErrorDetail(String errorCode, String message) {
            this(errorCode, message, null);
        }

        /**
         * Creates error detail from an exception.
         */
        public static ErrorDetail of(Exception e) {
            return new ErrorDetail(
                e.getClass().getSimpleName(),
                e.getMessage() != null ? e.getMessage() : "Unknown error",
                null
            );
        }

        /**
         * Creates error detail with all fields.
         */
        public static ErrorDetail of(String errorCode, String message, java.util.Map<String, Object> details) {
            return new ErrorDetail(errorCode, message, details);
        }
    }
}
