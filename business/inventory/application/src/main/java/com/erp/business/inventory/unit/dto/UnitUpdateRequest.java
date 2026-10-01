package com.erp.business.inventory.unit.dto;

import com.erp.business.inventory.domain.UnitDimension;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating a unit, used by both PUT and PATCH.
 *
 * <p>This is a mutable class rather than a record for one reason: Jackson needs
 * setters to distinguish a field the client omitted from one it sent as
 * {@code null}. With a record both arrive as a plain {@code null}, so a PATCH
 * could no longer say "leave the code alone but clear the symbol".
 *
 * <p>Per {@code Unit.update()}, an omitted required field means "leave it
 * untouched" while an explicit {@code null} on {@code symbol} means "clear it".
 *
 * <p>{@link JsonIgnore} on the getters keeps the record-style accessor from being
 * treated as an extra JSON property on serialisation.
 */
public class UnitUpdateRequest {

    @Size(max = 100, message = "Unit name must not exceed 100 characters")
    private String name;

    @Size(max = 32, message = "Unit code must not exceed 32 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9][A-Za-z0-9_/\\-]*$",
            message = "Unit code may contain only letters, digits, spaces, and the characters _ - /"
    )
    private String code;

    private UnitDimension dimension;

    @Size(max = 16, message = "Unit symbol must not exceed 16 characters")
    private String symbol;

    @Min(value = 0, message = "Decimal scale cannot be negative")
    @Max(value = 6, message = "Decimal scale must not exceed 6 digits")
    private Integer decimalScale;

    public UnitUpdateRequest() {
    }

    @JsonIgnore
    public String name() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String code() {
        return code;
    }

    @JsonSetter("code")
    public void setCode(String code) {
        this.code = code;
    }

    @JsonIgnore
    public UnitDimension dimension() {
        return dimension;
    }

    @JsonSetter("dimension")
    public void setDimension(UnitDimension dimension) {
        this.dimension = dimension;
    }

    @JsonIgnore
    public String symbol() {
        return symbol;
    }

    @JsonSetter("symbol")
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @JsonIgnore
    public Integer decimalScale() {
        return decimalScale;
    }

    @JsonSetter("decimalScale")
    public void setDecimalScale(Integer decimalScale) {
        this.decimalScale = decimalScale;
    }
}
