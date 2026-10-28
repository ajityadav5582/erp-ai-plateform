package com.erp.business.pos.register;

import com.erp.business.pos.domain.PosRegisterStatus;
import com.erp.business.pos.register.dto.CreatePosRegisterRequest;
import com.erp.business.pos.register.dto.PosRegisterResponse;
import com.erp.business.pos.register.dto.UpdatePosRegisterRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * POS register endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, enforced by the shared aspect. The controller only
 * reads the authenticated tenant and company, so a caller cannot widen its own
 * scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/pos/registers")
@RequiredArgsConstructor
public class PosRegisterController {

    private static final String BASE_PATH = "/api/v1/pos/registers";

    private final PosRegisterService registerService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PosRegisterResponse> createRegister(
            @Valid @RequestBody CreatePosRegisterRequest request,
            UriComponentsBuilder uriBuilder) {

        PosRegisterResponse response = registerService.createRegister(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PosRegisterResponse>> listRegisters(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(registerService.listRegisters(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PosRegisterResponse>> searchRegisters(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(registerService.searchRegisters(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<PosRegisterResponse>> listRegistersByStatus(
            @RequestParam("status") PosRegisterStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(registerService.listRegistersByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PosRegisterResponse>> listActiveRegisters() {
        return ResponseEntity.ok(registerService.listActiveRegisters(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-code")
    public ResponseEntity<PosRegisterResponse> getRegisterByCode(@RequestParam("code") String code) {
        return ResponseEntity.ok(registerService.getRegisterByCode(
                context.getTenantId(), context.getCompanyId(), code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosRegisterResponse> getRegisterById(@PathVariable Long id) {
        return ResponseEntity.ok(registerService.getRegisterById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PosRegisterResponse> updateRegister(
            @PathVariable Long id, @Valid @RequestBody UpdatePosRegisterRequest request) {
        return ResponseEntity.ok(registerService.updateRegister(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PosRegisterResponse> activateRegister(@PathVariable Long id) {
        return ResponseEntity.ok(registerService.activateRegister(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<PosRegisterResponse> deactivateRegister(@PathVariable Long id) {
        return ResponseEntity.ok(registerService.deactivateRegister(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
