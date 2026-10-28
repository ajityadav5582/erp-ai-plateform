package com.erp.business.sales.customer;

import com.erp.business.sales.customer.domain.CustomerType;
import com.erp.business.sales.customer.dto.CreateCustomerRequest;
import com.erp.business.sales.customer.dto.CustomerResponse;
import com.erp.business.sales.customer.dto.UpdateCustomerRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Customer master endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final String BASE_PATH = "/api/v1/sales/customers";

    private final CustomerService customerService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            UriComponentsBuilder uriBuilder) {

        CustomerResponse response = customerService.createCustomer(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(customerService.listCustomers(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam("name") String name,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(
                context.getTenantId(), context.getCompanyId(), name, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<java.util.List<CustomerResponse>> listActiveCustomers() {
        return ResponseEntity.ok(customerService.listActiveCustomers(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-type")
    public ResponseEntity<java.util.List<CustomerResponse>> listCustomersByType(
            @RequestParam("type") CustomerType type) {
        return ResponseEntity.ok(customerService.listCustomersByType(
                context.getTenantId(), context.getCompanyId(), type));
    }

    @GetMapping("/by-code")
    public ResponseEntity<CustomerResponse> getCustomerByCode(@RequestParam("code") String code) {
        return ResponseEntity.ok(customerService.getCustomerByCode(
                context.getTenantId(), context.getCompanyId(), code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CustomerResponse> activateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.activateCustomer(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CustomerResponse> deactivateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.deactivateCustomer(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(context.getTenantId(), context.getCompanyId(), id);
        return ResponseEntity.noContent().build();
    }
}
