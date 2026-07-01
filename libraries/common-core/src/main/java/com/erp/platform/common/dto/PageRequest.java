package com.erp.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard pagination request parameters.
 *
 * <p>Provides consistent pagination across all list endpoints.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ParameterObject
@Schema(description = "Pagination parameters")
public record PageRequest(
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    @Min(value = 0, message = "Page number must be >= 0")
    Integer page,

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(value = 1, message = "Page size must be >= 1")
    @Min(value = 100, message = "Page size must be <= 100")
    Integer size,

    @Schema(description = "Sort field and direction", example = "createdAt,desc")
    @Size(max = 10, message = "Maximum 10 sort fields allowed")
    List<String> sort
) implements Serializable {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * Creates a PageRequest with defaults.
     */
    public PageRequest() {
        this(DEFAULT_PAGE, DEFAULT_SIZE, new ArrayList<>());
    }

    /**
     * Creates a PageRequest with page and size.
     */
    public PageRequest(Integer page, Integer size) {
        this(page, size, new ArrayList<>());
    }

    /**
     * Returns the page number with default applied.
     */
    public int getPageOrDefault() {
        return page != null ? page : DEFAULT_PAGE;
    }

    /**
     * Returns the page size with default and max applied.
     */
    public int getSizeOrDefault() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Returns sort parameters as Sort objects.
     */
    public List<Sort> getSorts() {
        if (sort == null || sort.isEmpty()) {
            return List.of(Sort.byDesc("createdAt"));
        }

        return sort.stream()
            .map(Sort::fromString)
            .toList();
    }

    /**
     * Returns the offset for database queries.
     */
    public long getOffset() {
        return (long) getPageOrDefault() * getSizeOrDefault();
    }

    /**
     * Sort direction.
     */
    public enum Direction {
        ASC, DESC;

        public static Direction from(String direction) {
            return Direction.valueOf(direction.toUpperCase());
        }
    }

    /**
     * Sort parameter.
     */
    public record Sort(
        String property,
        Direction direction
    ) implements Serializable {
        private static final String DEFAULT_PROPERTY = "createdAt";
        private static final Direction DEFAULT_DIRECTION = Direction.DESC;

        /**
         * Creates a Sort with default direction.
         */
        public Sort(String property) {
            this(property, DEFAULT_DIRECTION);
        }

        /**
         * Parses a sort string like "createdAt,desc".
         */
        public static Sort fromString(String sortString) {
            String[] parts = sortString.split(",");
            String property = parts[0].trim();
            Direction direction = parts.length > 1
                ? Direction.from(parts[1].trim())
                : DEFAULT_DIRECTION;
            return new Sort(property, direction);
        }

        /**
         * Returns ascending sort by property.
         */
        public static Sort by(String property) {
            return new Sort(property, Direction.ASC);
        }

        /**
         * Returns descending sort by property.
         */
        public static Sort byDesc(String property) {
            return new Sort(property, Direction.DESC);
        }
    }
}
