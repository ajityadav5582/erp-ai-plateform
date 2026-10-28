package com.erp.business.sales.customer;

import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.domain.CustomerType;
import com.erp.business.sales.customer.dto.CreateCustomerRequest;
import com.erp.business.sales.customer.dto.CustomerResponse;
import com.erp.business.sales.customer.dto.UpdateCustomerRequest;
import com.erp.business.sales.customer.exception.CustomerNotFoundException;
import com.erp.business.sales.customer.exception.DuplicateCustomerCodeException;
import com.erp.business.sales.customer.exception.DuplicateCustomerNameException;
import com.erp.business.sales.customer.infrastructure.persistence.CustomerRepository;
import com.erp.business.sales.customer.mapper.CustomerMapper;
import com.erp.business.sales.shared.security.CustomerPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer master use-case implementation.
 *
 * <p>Authorisation is declared per method with {@code @RequiresPermission} and
 * enforced by the shared aspect before the body runs, so an unauthorised caller
 * never reaches the repository. Uniqueness of code and name is checked here before
 * the save and again by the database unique constraints, so a race between two
 * concurrent creates surfaces as a conflict rather than a duplicate row.
 */
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @RequiresPermission(CustomerPermissions.CUSTOMER_CREATE)
    public CustomerResponse createCustomer(Long tenantId, Long companyId, CreateCustomerRequest request) {
        String code = normalizeCode(request.code());
        if (customerRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
            throw new DuplicateCustomerCodeException(
                    "Customer with code '" + code + "' already exists in this company");
        }

        String name = request.name().trim();
        if (customerRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
            throw new DuplicateCustomerNameException(
                    "Customer with name '" + name + "' already exists in this company");
        }

        Customer customer = Customer.create(
                tenantId, companyId, name, code, request.type(),
                request.email(), request.phone(), request.address(),
                request.panNumber(), Boolean.TRUE.equals(request.vatRegistered()),
                request.creditLimit(), request.paymentTermsDays(), request.currency()
        );

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public CustomerResponse getCustomerById(Long tenantId, Long companyId, Long id) {
        Customer customer = customerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public CustomerResponse getCustomerByCode(Long tenantId, Long companyId, String code) {
        Customer customer = customerRepository
                .findByTenantIdAndCompanyIdAndCode(tenantId, companyId, normalizeCode(code))
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with code: " + code));
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public Page<CustomerResponse> listCustomers(Long tenantId, Long companyId, Pageable pageable) {
        return customerRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(customerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public Page<CustomerResponse> searchCustomers(Long tenantId, Long companyId, String name, Pageable pageable) {
        return customerRepository.searchByName(tenantId, companyId, name, pageable)
                .map(customerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public List<CustomerResponse> listActiveCustomers(Long tenantId, Long companyId) {
        return customerRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public List<CustomerResponse> listCustomersByType(Long tenantId, Long companyId, CustomerType type) {
        return customerRepository.findByTenantIdAndCompanyIdAndType(tenantId, companyId, type).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @RequiresPermission(CustomerPermissions.CUSTOMER_UPDATE)
    public CustomerResponse updateCustomer(Long tenantId, Long companyId, Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        if (request.code() != null) {
            String newCode = normalizeCode(request.code());
            if (!newCode.equals(customer.getCode())
                    && customerRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, newCode)) {
                throw new DuplicateCustomerCodeException(
                        "Customer with code '" + newCode + "' already exists in this company");
            }
        }
        if (request.name() != null) {
            String newName = request.name().trim();
            if (!newName.equals(customer.getName())
                    && customerRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, newName)) {
                throw new DuplicateCustomerNameException(
                        "Customer with name '" + newName + "' already exists in this company");
            }
        }

        customer.update(
                request.name(), request.code(), request.type(),
                request.email(), request.phone(), request.address(),
                request.panNumber(), Boolean.TRUE.equals(request.vatRegistered()),
                request.creditLimit(), request.paymentTermsDays(), request.currency()
        );

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @RequiresPermission(CustomerPermissions.CUSTOMER_UPDATE)
    public CustomerResponse activateCustomer(Long tenantId, Long companyId, Long id) {
        Customer customer = customerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
        customer.activate();
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @RequiresPermission(CustomerPermissions.CUSTOMER_UPDATE)
    public CustomerResponse deactivateCustomer(Long tenantId, Long companyId, Long id) {
        Customer customer = customerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
        customer.deactivate();
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @RequiresPermission(CustomerPermissions.CUSTOMER_DELETE)
    public void deleteCustomer(Long tenantId, Long companyId, Long id) {
        Customer customer = customerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
        customerRepository.delete(customer);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
