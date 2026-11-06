package com.erp.business.pos.session;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/sessions")
public class SessionCloseController {

    private final SessionCloseService service;

    public SessionCloseController(SessionCloseService service) {
        this.service = service;
    }

    @PostMapping("/{id}/close-session")
    public SessionCloseResponse close(@PathVariable Long id, @RequestBody SessionCloseRequest request) {
        return service.close(id, request);
    }
}
