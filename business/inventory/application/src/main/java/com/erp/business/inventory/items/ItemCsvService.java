package com.erp.business.inventory.items;

import com.erp.business.inventory.items.dto.ItemImportResult;

import java.io.InputStream;

public interface ItemCsvService {

    byte[] generateTemplateCsv();

    byte[] exportItemsCsv(Long tenantId, Long companyId);

    ItemImportResult importItemsCsv(Long tenantId, Long companyId, InputStream inputStream);
}

