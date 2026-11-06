package com.erp.business.procurement.purchaseinvoice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseTaxCategoryTest {
    @Test
    void allValuesExist() {
        assertEquals(5, PurchaseTaxCategory.values().length);
    }
}
