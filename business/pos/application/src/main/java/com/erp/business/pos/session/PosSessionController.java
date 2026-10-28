package com.erp.business.pos.session;

import com.erp.business.pos.domain.PosSessionStatus;
import com.erp.business.pos.session.dto.CloseSessionRequest;
import com.erp.business.pos.session.dto.OpenSessionRequest;
import com.erp.business.pos.session.dto.PosSessionResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * POS cashier session endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, enforced by the shared aspect. The controller only
 * reads the authenticated tenant and company, so a caller cannot widen its own
 * scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/pos/sessions")
@RequiredArgsConstructor
public class PosSessionController {

    private static final String BASE_PATH = "/api/v1/pos/sessions";

    private final PosSessionService sessionService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PosSessionResponse> openSession(
            @Valid @RequestBody OpenSessionRequest request,
            UriComponentsBuilder uriBuilder) {

        PosSessionResponse response = sessionService.openSession(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PosSessionResponse>> listSessions(
            @PageableDefault(size = 20, sort = {"openedAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.listSessions(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PosSessionResponse>> searchSessions(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"openedAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.searchSessions(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<PosSessionResponse>> listSessionsByStatus(
            @RequestParam("status") PosSessionStatus status,
            @PageableDefault(size = 20, sort = {"openedAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.listSessionsByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-register")
    public ResponseEntity<Page<PosSessionResponse>> listSessionsByRegister(
            @RequestParam("registerId") Long registerId,
            @PageableDefault(size = 20, sort = {"openedAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.listSessionsByRegister(
                context.getTenantId(), context.getCompanyId(), registerId, pageable));
    }

    @GetMapping("/open-for-register")
    public ResponseEntity<PosSessionResponse> findOpenSessionForRegister(@RequestParam("registerId") Long registerId) {
        return sessionService
                .findOpenSessionForRegister(context.getTenantId(), context.getCompanyId(), registerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/my-open")
    public ResponseEntity<List<PosSessionResponse>> listOpenSessionsForCurrentCashier() {
        return ResponseEntity.ok(sessionService.listOpenSessionsForCurrentCashier(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-number")
    public ResponseEntity<PosSessionResponse> getSessionByNumber(@RequestParam("number") String number) {
        return ResponseEntity.ok(sessionService.getSessionByNumber(
                context.getTenantId(), context.getCompanyId(), number));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosSessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PosSessionResponse> closeSession(
            @PathVariable Long id, @Valid @RequestBody CloseSessionRequest request) {
        return ResponseEntity.ok(sessionService.closeSession(
                context.getTenantId(), context.getCompanyId(), id, request));
    }
}
