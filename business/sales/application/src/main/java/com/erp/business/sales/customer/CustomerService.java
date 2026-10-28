package com.erp.business.sales.customer;

import com.erp.business.sales.customer.domain.CustomerType;
import com.erp.business.sales.customer.dto.CreateCustomerRequest;
import com.erp.business.sales.customer.dto.CustomerResponse;
import com.erp.business.sales.customer.dto.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Customer master use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface CustomerService {

    CustomerResponse createCustomer(Long tenantId, Long companyId, CreateCustomerRequest request);

    CustomerResponse getCustomerById(Long tenantId, Long companyId, Long id);

    CustomerResponse getCustomerByCode(Long tenantId, Long companyId, String code);

    Page<CustomerResponse> listCustomers(Long tenantId, Long companyId, Pageable pageable);

    Page<CustomerResponse> searchCustomers(Long tenantId, Long companyId, String name, Pageable pageable);

    List<CustomerResponse> listActiveCustomers(Long tenantId, Long companyId);

    List<CustomerResponse> listCustomersByType(Long tenantId, Long companyId, CustomerType type);

    CustomerResponse updateCustomer(Long tenantId, Long companyId, Long id, UpdateCustomerRequest request);

    CustomerResponse activateCustomer(Long tenantId, Long companyId, Long id);

    CustomerResponse deactivateCustomer(Long tenantId, Long companyId, Long id);

    void deleteCustomer(Long tenantId, Long companyId, Long id);
}
