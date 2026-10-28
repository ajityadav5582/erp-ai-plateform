package com.erp.platform.identity.infrastructure.initializer;

import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.domain.CompanyFiscalYear;
import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.infrastructure.persistence.CompanyFiscalYearRepository;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import com.erp.platform.identity.infrastructure.persistence.MasterFiscalYearRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeds sample dummy data for development and testing.
 *
 * <p>This initializer runs after the platform-wide {@link DataInitializer} and
 * creates tenant-specific sample data including:
 * <ul>
 *   <li>Sample tenants</li>
 *   <li>Sample companies per tenant</li>
 *   <li>Sample users with different roles per company</li>
 *   <li>Company fiscal years linked to master fiscal years</li>
 * </ul>
 *
 * <p>The initializer is idempotent: it checks for existing data before creating
 * new records, so it can be run multiple times safely.
 *
 * <p>To enable this initializer, set the Spring profile to {@code dev} or
 * {@code local}, or set the environment variable {@code SEED_DUMMY_DATA=true}.
 */
@Component
@Order(2) // Run after DataInitializer (Order 1)
public class DummyDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DummyDataInitializer.class);
    private static final String SEED_ASSIGNED_BY = "dummy-data-initializer";
    private static final String DEFAULT_PASSWORD = "Password@123";

    private final TenantRepository tenantRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final MasterFiscalYearRepository masterFiscalYearRepository;
    private final CompanyFiscalYearRepository companyFiscalYearRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public DummyDataInitializer(
            TenantRepository tenantRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            MasterFiscalYearRepository masterFiscalYearRepository,
            CompanyFiscalYearRepository companyFiscalYearRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.masterFiscalYearRepository = masterFiscalYearRepository;
        this.companyFiscalYearRepository = companyFiscalYearRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Only run if explicitly enabled
        String seedDummy = System.getenv("SEED_DUMMY_DATA");
        if (seedDummy == null || !Boolean.parseBoolean(seedDummy)) {
            log.info("Dummy data seeding is disabled. Set SEED_DUMMY_DATA=true to enable.");
            return;
        }

        log.info("Starting dummy data seeding...");

        // Step 1: Create tenants
        List<Tenant> tenants = ensureSampleTenantsExist();

        // Step 2: Create companies for each tenant
        for (Tenant tenant : tenants) {
            List<Company> companies = ensureSampleCompaniesExist(tenant);

            // Step 3: Create company fiscal years
            for (Company company : companies) {
                ensureCompanyFiscalYearsExist(tenant.getId(), company);
            }

            // Step 4: Create users and assign roles
            for (Company company : companies) {
                ensureSampleUsersExist(tenant.getId(), company);
            }
        }

        log.info("Dummy data seeding completed successfully!");
    }

    private List<Tenant> ensureSampleTenantsExist() {
        List<Tenant> tenants = new ArrayList<>();

        // Tenant 1: Tech Solutions
        Optional<Tenant> existingTenant1 = tenantRepository.findByTenantCode("TECH-SOL-001");
        Tenant tenant1 = existingTenant1.orElseGet(() -> {
            Tenant tenant = Tenant.create(
                    "TECH-SOL-001",
                    "Tech Solutions Pvt. Ltd.",
                    "Tech Solutions",
                    "admin@techsolutions.com",
                    "+9779812345678",
                    "https://techsolutions.com",
                    "UTC",
                    "USD",
                    "en",
                    com.erp.platform.tenant.domain.IsolationStrategy.SCHEMA_PER_TENANT
            );
            tenant.activate(Instant.now());
            return tenantRepository.save(tenant);
        });
        tenants.add(tenant1);

        // Tenant 2: Global Trading
        Optional<Tenant> existingTenant2 = tenantRepository.findByTenantCode("GLOB-TRAD-002");
        Tenant tenant2 = existingTenant2.orElseGet(() -> {
            Tenant tenant = Tenant.create(
                    "GLOB-TRAD-002",
                    "Global Trading Company",
                    "Global Trading",
                    "admin@globaltrading.com",
                    "+9779812345679",
                    "https://globaltrading.com",
                    "UTC",
                    "USD",
                    "en",
                    com.erp.platform.tenant.domain.IsolationStrategy.SCHEMA_PER_TENANT
            );
            tenant.activate(Instant.now());
            return tenantRepository.save(tenant);
        });
        tenants.add(tenant2);

        log.info("Ensured {} tenants exist", tenants.size());
        return tenants;
    }

    private List<Company> ensureSampleCompaniesExist(Tenant tenant) {
        List<Company> companies = new ArrayList<>();

        if (tenant.getTenantCode().equals("TECH-SOL-001")) {
            // Company 1: Tech Solutions Kathmandu
            Optional<Company> existingCompany1 = companyRepository.findByTenantIdAndCompanyCode(tenant.getId(), "TECH-KTM");
            Company company1 = existingCompany1.orElseGet(() -> {
                Company company = Company.create(
                        tenant.getId(),
                        "TECH-KTM",
                        "Tech Solutions Kathmandu",
                        "123456789",
                        "Kathmandu",
                        "+9779812345678",
                        "ktm@techsolutions.com",
                        CompanyType.PRIVATE_LIMITED,
                        BusinessType.OTHER,
                        FiscalYearType.NEPALI_BS,
                        true
                );
                return companyRepository.save(company);
            });
            companies.add(company1);

            // Company 2: Tech Solutions Pokhara
            Optional<Company> existingCompany2 = companyRepository.findByTenantIdAndCompanyCode(tenant.getId(), "TECH-PKH");
            Company company2 = existingCompany2.orElseGet(() -> {
                Company company = Company.create(
                        tenant.getId(),
                        "TECH-PKH",
                        "Tech Solutions Pokhara",
                        "987654321",
                        "Pokhara",
                        "+9779812345670",
                        "pkh@techsolutions.com",
                        CompanyType.PRIVATE_LIMITED,
                        BusinessType.OTHER,
                        FiscalYearType.NEPALI_BS,
                        true
                );
                return companyRepository.save(company);
            });
            companies.add(company2);
        } else if (tenant.getTenantCode().equals("GLOB-TRAD-002")) {
            // Company 3: Global Trading Kathmandu
            Optional<Company> existingCompany3 = companyRepository.findByTenantIdAndCompanyCode(tenant.getId(), "GLOB-KTM");
            Company company3 = existingCompany3.orElseGet(() -> {
                Company company = Company.create(
                        tenant.getId(),
                        "GLOB-KTM",
                        "Global Trading Kathmandu",
                        "456789123",
                        "Kathmandu",
                        "+9779812345680",
                        "ktm@globaltrading.com",
                        CompanyType.PRIVATE_LIMITED,
                        BusinessType.OTHER,
                        FiscalYearType.ENGLISH_AD,
                        true
                );
                return companyRepository.save(company);
            });
            companies.add(company3);

            // Company 4: Global Trading Biratnagar
            Optional<Company> existingCompany4 = companyRepository.findByTenantIdAndCompanyCode(tenant.getId(), "GLOB-BIR");
            Company company4 = existingCompany4.orElseGet(() -> {
                Company company = Company.create(
                        tenant.getId(),
                        "GLOB-BIR",
                        "Global Trading Biratnagar",
                        "789123456",
                        "Biratnagar",
                        "+9779812345681",
                        "bir@globaltrading.com",
                        CompanyType.PRIVATE_LIMITED,
                        BusinessType.OTHER,
                        FiscalYearType.ENGLISH_AD,
                        true
                );
                return companyRepository.save(company);
            });
            companies.add(company4);
        }

        log.info("Ensured {} companies exist for tenant {}", companies.size(), tenant.getTenantCode());
        return companies;
    }

    private void ensureCompanyFiscalYearsExist(Long tenantId, Company company) {
        List<MasterFiscalYear> masterFiscalYears = masterFiscalYearRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        // Select 3 fiscal years for each company
        List<MasterFiscalYear> selectedFiscalYears = masterFiscalYears.stream()
                .filter(mf -> mf.getCode().matches("208[0-2]-[0-9]{2}"))
                .limit(3)
                .toList();

        for (MasterFiscalYear masterFy : selectedFiscalYears) {
            String code = masterFy.getCode();
            String name = masterFy.getName();

            // Check if this fiscal year already exists for the company
            Long count = entityManager.createQuery(
                            "select count(f) from CompanyFiscalYear f where f.companyId = :companyId and f.code = :code",
                            Long.class)
                    .setParameter("companyId", company.getId())
                    .setParameter("code", code)
                    .getSingleResult();

            if (count == 0) {
                CompanyFiscalYear companyFy = CompanyFiscalYear.create(
                        tenantId,
                        company.getId(),
                        masterFy.getId(),
                        name,
                        code,
                        masterFy.getStartDateBs(),
                        masterFy.getEndDateBs(),
                        masterFy.getStartDateAd(),
                        masterFy.getEndDateAd(),
                        company.getFiscalYearType()
                );
                companyFy.activate();
                companyFiscalYearRepository.save(companyFy);
            }
        }

        log.info("Ensured fiscal years exist for company {}", company.getCompanyCode());
    }

    private void ensureSampleUsersExist(Long tenantId, Company company) {
        String companyCode = company.getCompanyCode();
        String companySuffix = companyCode.substring(companyCode.lastIndexOf('-') + 1).toLowerCase();

        // Define users for this company
        String[][] users = {
                {"admin." + companySuffix + "@" + getTenantDomain(tenantId), "Admin", companySuffix.toUpperCase(), "Administrator"},
                {"manager." + companySuffix + "@" + getTenantDomain(tenantId), "Manager", companySuffix.toUpperCase(), "Manager"},
                {"accountant." + companySuffix + "@" + getTenantDomain(tenantId), "Accountant", companySuffix.toUpperCase(), "Accountant"},
                {"storekeeper." + companySuffix + "@" + getTenantDomain(tenantId), "Storekeeper", companySuffix.toUpperCase(), "Storekeeper"}
        };

        for (String[] userData : users) {
            String email = userData[0];
            String firstName = userData[1];
            String lastName = userData[2];
            String jobTitle = userData[3];

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user = existingUser.orElseGet(() -> {
                String username = email;
                String passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD);

                User newUser = User.create(
                        tenantId,
                        username,
                        email,
                        passwordHash,
                        firstName,
                        lastName,
                        null,
                        jobTitle,
                        null,
                        null,
                        null,
                        company.getId()
                );
                newUser.activate(Instant.now());
                return userRepository.save(newUser);
            });

            // Assign roles
            assignRoleToUser(user, tenantId, jobTitle);
        }

        log.info("Ensured sample users exist for company {}", company.getCompanyCode());
    }

    private String getTenantDomain(Long tenantId) {
        // Return the appropriate domain based on tenant ID
        if (tenantId == 1) {
            return "techsolutions.com";
        } else if (tenantId == 2) {
            return "globaltrading.com";
        }
        return "example.com";
    }

    private void assignRoleToUser(User user, Long tenantId, String jobTitle) {
        String roleCode;
        boolean isPrimary = true;

        switch (jobTitle) {
            case "Administrator":
                roleCode = "TENANT_ADMIN";
                break;
            case "Manager":
                roleCode = "MANAGER";
                break;
            case "Accountant":
                roleCode = "ACCOUNTANT";
                break;
            case "Storekeeper":
                roleCode = "STOREKEEPER";
                break;
            default:
                roleCode = "TENANT_ADMIN";
        }

        // Find the role
        Optional<Role> roleOpt = roleRepository.findByTenantIdAndRoleCodeAndRoleType(
                tenantId, roleCode, RoleType.CUSTOM);

        // For system roles, tenantId is null
        if (roleOpt.isEmpty() && (roleCode.equals("TENANT_ADMIN") || roleCode.equals("STOREKEEPER"))) {
            roleOpt = roleRepository.findByTenantIdAndRoleCodeAndRoleType(
                    null, roleCode, RoleType.SYSTEM);
        }

        if (roleOpt.isEmpty() && (roleCode.equals("MANAGER") || roleCode.equals("ACCOUNTANT"))) {
            // Create the custom role if it doesn't exist
            Role customRole = Role.createCustom(tenantId, roleCode, jobTitle, jobTitle + " role");
            roleOpt = Optional.of(roleRepository.save(customRole));
        }

        roleOpt.ifPresent(role -> {
            // Check if user role already exists
            Optional<UserRole> existingUserRole = userRoleRepository.findByUserIdAndTenantIdAndRoleId(
                    user.getId(), tenantId, role.getId());

            if (existingUserRole.isEmpty()) {
                UserRole userRole = UserRole.assign(
                        user.getId(),
                        role.getId(),
                        tenantId,
                        SEED_ASSIGNED_BY,
                        Instant.now(),
                        null,
                        isPrimary
                );
                userRoleRepository.save(userRole);
            }
        });
    }
}
