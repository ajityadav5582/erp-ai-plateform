package com.erp.business.inventory.supplier.dto;

import com.erp.business.inventory.domain.SupplierType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;

public class SupplierUpdateRequest {

    @Size(max = 255, message = "Supplier name must not exceed 255 characters")
    private String name;

    @Size(max = 64, message = "Supplier code must not exceed 64 characters")
    private String code;

    private SupplierType type;

    private String email;
    private String phone;
    private String address;
    private String taxId;
    private Integer paymentTermsDays;
    private String currency;
    private com.erp.business.inventory.domain.Supplier.VatRegistrationStatus vatRegistrationStatus;
    private String vatRegistrationNumber;
    private java.time.LocalDate vatEffectiveFrom;
    private java.time.LocalDate vatEffectiveTo;

    public com.erp.business.inventory.domain.Supplier.VatRegistrationStatus vatRegistrationStatus() { return vatRegistrationStatus; }
    @JsonSetter("vatRegistrationStatus")
    public void setVatRegistrationStatus(com.erp.business.inventory.domain.Supplier.VatRegistrationStatus v) { this.vatRegistrationStatus = v; }

    public String vatRegistrationNumber() { return vatRegistrationNumber; }
    @JsonSetter("vatRegistrationNumber")
    public void setVatRegistrationNumber(String v) { this.vatRegistrationNumber = v; }

    public java.time.LocalDate vatEffectiveFrom() { return vatEffectiveFrom; }
    @JsonSetter("vatEffectiveFrom")
    public void setVatEffectiveFrom(java.time.LocalDate v) { this.vatEffectiveFrom = v; }

    public java.time.LocalDate vatEffectiveTo() { return vatEffectiveTo; }
    @JsonSetter("vatEffectiveTo")
    public void setVatEffectiveTo(java.time.LocalDate v) { this.vatEffectiveTo = v; }

    public SupplierUpdateRequest() {
    }

    public String name() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    public String code() {
        return code;
    }

    @JsonSetter("code")
    public void setCode(String code) {
        this.code = code;
    }

    public SupplierType type() {
        return type;
    }

    @JsonSetter("type")
    public void setType(SupplierType type) {
        this.type = type;
    }

    public String email() {
        return email;
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
    }

    public String phone() {
        return phone;
    }

    @JsonSetter("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String address() {
        return address;
    }

    @JsonSetter("address")
    public void setAddress(String address) {
        this.address = address;
    }

    public String taxId() {
        return taxId;
    }

    @JsonSetter("taxId")
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public Integer paymentTermsDays() {
        return paymentTermsDays;
    }

    @JsonSetter("paymentTermsDays")
    public void setPaymentTermsDays(Integer paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }

    public String currency() {
        return currency;
    }

    @JsonSetter("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
