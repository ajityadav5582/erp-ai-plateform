package com.erp.business.pos.receipt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/transactions")
public class ReprintController {

    private final ReprintService reprintService;

    public ReprintController(ReprintService reprintService) {
        this.reprintService = reprintService;
    }

    @PostMapping("/{id}/reprint")
    public ReprintResponseDto reprint(@PathVariable Long id) {
        return reprintService.reprint(id, null, null);
    }
}
