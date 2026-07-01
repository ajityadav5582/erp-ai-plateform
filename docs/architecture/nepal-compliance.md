# Nepal Compliance Pack Architecture

## 1. Purpose

This document defines the Nepal-specific compliance capabilities that enable the ERP platform to operate legally and efficiently in Nepal. These capabilities are designed as reusable, country-specific modules that can be extended to support other countries.

## 2. Design Principles

- **Country-Agnostic Core:** Business modules don't know about Nepal-specific rules
- **Pluggable Compliance:** Country packs plug into the platform
- **Reusable Components:** Compliance features shared across modules
- **Configurable Rules:** Business rules defined in configuration, not code
- **Audit Ready:** Complete audit trail for all compliance operations
- **Future Expandable:** Easy to add new countries

## 3. Nepal Compliance Services

### 3.1 IRD (Inland Revenue Department) Integration Service

**Purpose:** Integration with Nepal's tax authority for reporting and compliance.

**Responsibilities:**
- IRD API integration
- Invoice submission to IRD
- Tax return filing
- IRD acknowledgment handling
- IRD error handling and retry
- IRD submission audit trail
- IRD certificate management
- IRD sandbox/production environment switching

**Why it exists:**
- Legal requirement for tax reporting in Nepal
- Automated compliance reduces manual work
- Real-time validation against IRD rules
- Audit trail for tax submissions

**Dependencies:**
- Country Configuration Service (IRD settings)
- Accounting Service (tax data)
- Sales Service (invoice data)
- Audit Service (submission tracking)

**APIs:**
- `/api/v1/compliance/nepal/ird/submit-invoice` - Submit invoice to IRD
- `/api/v1/compliance/nepal/ird/submit-return` - Submit tax return
- `/api/v1/compliance/nepal/ird/status` - Check submission status
- `/api/v1/compliance/nepal/ird/certificates` - Manage IRD certificates

---

### 3.2 VAT (Value Added Tax) Service

**Purpose:** Manage VAT calculation, reporting, and compliance.

**Responsibilities:**
- VAT rate management (standard, reduced, zero, exempt)
- VAT calculation on transactions
- VAT invoice formatting (mandatory fields)
- VAT return preparation
- VAT reconciliation
- VAT input/output tracking
- VAT audit report generation
- VAT payment scheduling

**Why it exists:**
- VAT is mandatory for most businesses in Nepal
- Complex VAT rules require specialized handling
- VAT reporting has strict format requirements
- VAT reconciliation is time-consuming

**Dependencies:**
- Country Configuration Service (VAT settings)
- Accounting Service (VAT accounts)
- Sales Service (sales VAT)
- Procurement Service (purchase VAT)
- Report Service (VAT reports)

**VAT Rates (Nepal):**
| Type | Rate | Applicable To |
|------|------|---------------|
| Standard | 13% | Most goods and services |
| Reduced | 5% | Essential goods |
| Zero | 0% | Export, special zones |
| Exempt | - | Healthcare, education |

**VAT Invoice Requirements:**
- VAT registration number (PAN)
- Customer VAT number (if registered)
- VAT rate and amount per line
- Total VAT amount
- IRD-mandated invoice format

---

### 3.3 PAN (Permanent Account Number) Service

**Purpose:** Manage PAN registration, validation, and compliance.

**Responsibilities:**
- PAN format validation
- PAN verification against IRD
- PAN-based customer classification
- PAN on document generation
- PAN change tracking
- PAN-based reporting
- PAN audit trail

**Why it exists:**
- PAN is mandatory for all business transactions in Nepal
- PAN validation prevents fraud
- PAN-based classification affects tax treatment

**Dependencies:**
- Country Configuration Service (PAN settings)
- CRM Service (customer PAN)
- Sales Service (buyer PAN)
- Procurement Service (vendor PAN)

**PAN Format (Nepal):**
- Format: 9 digits (e.g., 123456789)
- Structure: [Office Code][Registration Number][Check Digit]
- Validation: IRD API verification

---

### 3.4 TDS (Tax Deducted at Source) Service

**Purpose:** Manage TDS calculation, deduction, and remittance.

**Responsibilities:**
- TDS rate management by payment type
- TDS calculation on payments
- TDS certificate generation
- TDS return preparation
- TDS remittance tracking
- TDS reconciliation
- TDS on salaries, vendors, contractors
- TDS exemption management

**Why it exists:**
- TDS is mandatory for certain payments in Nepal
- Complex TDS rules by payment type
- TDS certificates required for vendors
- TDS reconciliation is complex

**Dependencies:**
- Country Configuration Service (TDS settings)
- Accounting Service (TDS accounts)
- HR Service (salary TDS)
- Procurement Service (vendor TDS)
- Report Service (TDS reports)

**TDS Rates (Nepal - Examples):**
| Payment Type | Rate | Threshold |
|--------------|------|-----------|
| Professional fees | 15% | NPR 50,000/year |
| Rent | 15% | NPR 200,000/year |
| Interest | 15% | NPR 25,000/year |
| Salary | Progressive | As per slab |
| Contractor | 15% | NPR 300,000/year |

---

### 3.5 SSF (Social Security Fund) Service

**Purpose:** Manage SSF contribution calculation and reporting.

**Responsibilities:**
- SSF rate management
- SSF contribution calculation
- SSF monthly return preparation
- SSF payment tracking
- SSF employee registration
- SSF employer registration
- SSF reconciliation
- SSF certificate generation

**Why it exists:**
- SSF is mandatory for employers in Nepal
- SSF contributions are calculated on salaries
- SSF reporting has specific requirements
- SSF compliance requires tracking

**Dependencies:**
- Country Configuration Service (SSF settings)
- HR Service (salary data)
- Accounting Service (SSF accounts)
- Report Service (SSF reports)

**SSF Rates (Nepal):**
| Contribution | Employer | Employee |
|--------------|----------|----------|
| SSF | 10% of basic | 10% of basic |
| Gratuity | 8.33% of basic | - |

---

### 3.6 CIT (Corporate Income Tax) Service

**Purpose:** Manage corporate income tax calculation and compliance.

**Responsibilities:**
- CIT rate management
- CIT calculation on profits
- CIT advance tax calculation
- CIT return preparation
- CIT payment scheduling
- Tax incentive management
- CIT reconciliation
- CIT audit support

**Why it exists:**
- CIT is mandatory for companies in Nepal
- CIT calculation requires detailed financial data
- CIT has quarterly advance tax requirements
- CIT incentives need tracking

**Dependencies:**
- Country Configuration Service (CIT settings)
- Accounting Service (profit data)
- Report Service (CIT reports)

**CIT Rates (Nepal):**
| Type | Rate |
|------|------|
| Standard corporate | 25% |
| Bank/financial institutions | 30% |
| Special industries | 10-20% |

---

### 3.7 Fiscal Year Service

**Purpose:** Manage fiscal year configuration and operations specific to Nepal.

**Responsibilities:**
- Fiscal year definition (Nepal: Shrawan-Ashad)
- Fiscal year calendar management
- Period opening and closing
- Fiscal year transition
- Fiscal year comparison
- Fiscal year-based reporting
- Fiscal year audit trail

**Why it exists:**
- Nepal uses a different fiscal year (July-June)
- Fiscal year affects all financial operations
- Period closing is mandatory for compliance
- Fiscal year transitions require special handling

**Nepal Fiscal Year:**
- Start: Shrawan 1 (mid-July)
- End: Ashad 30 (mid-July)
- Months: 12 (Nepali calendar)
- Quarters: 4 (based on Nepali seasons)

**Fiscal Year Operations:**
- Period opening: Unlock periods for data entry
- Period closing: Lock periods after closing
- Year-end closing: Final closing procedures
- New year opening: Setup for new fiscal year

---

### 3.8 Nepali Calendar (Bikram Sambat) Service

**Purpose:** Convert between Gregorian and Bikram Sambat calendars. Enable Nepali date operations.

**Responsibilities:**
- Date conversion (AD ↔ BS)
- Nepali date formatting
- Nepali month and day names
- Nepali calendar calculations
- Date validation in BS
- Fiscal year calculation in BS
- Holiday calculation in BS

**Why it exists:**
- Nepal officially uses Bikram Sambat calendar
- Government documents require BS dates
- Fiscal year defined in BS
- Business operations use both calendars

**Calendar Conversion:**
```
Gregorian (AD) ↔ Bikram Sambat (BS)
Example: 2024-01-15 AD ↔ 2080-10-01 BS
```

**Nepali Months:**
| BS Month | Approx. AD |
|----------|------------|
| Baisakh | April-May |
| Jestha | May-June |
| Ashadh | June-July |
| Shrawan | July-August |
| Bhadra | August-September |
| Ashoj | September-October |
| Kartik | October-November |
| Mangsir | November-December |
| Poush | December-January |
| Magh | January-February |
| Falgun | February-March |
| Chaitra | March-April |

---

### 3.9 Invoice Rules Service

**Purpose:** Enforce Nepal-specific invoice rules and formatting requirements.

**Responsibilities:**
- Invoice number format enforcement
- Mandatory field validation
- Invoice sequence management
- IRD invoice format compliance
- Invoice printing format
- Invoice cancellation rules
- Invoice amendment rules
- Invoice retention requirements

**Why it exists:**
- IRD has strict invoice formatting requirements
- Invoice numbers must follow specific patterns
- Certain fields are mandatory
- Invoices must be retained for specific periods

**Invoice Requirements (Nepal):**
- Invoice number: Sequential, unique per fiscal year
- Date: In BS or AD (as per preference)
- Seller: Name, PAN, address
- Buyer: Name, PAN (if registered), address
- Items: Description, quantity, rate, amount, VAT
- Totals: Subtotal, VAT, Grand total
- Signature: Authorized signatory

**Invoice Types:**
- Sales Invoice (VAT)
- Sales Invoice (Non-VAT)
- Export Invoice
- Debit Note
- Credit Note

---

### 3.10 Government Reports Service

**Purpose:** Generate and submit all mandatory government reports for Nepal.

**Responsibilities:**
- VAT return generation
- TDS return generation
- SSF return generation
- CIT return generation
- Annual audit report preparation
- Government report formatting
- Report submission to IRD
- Report archival

**Why it exists:**
- Multiple government reports are mandatory
- Reports have specific formats and deadlines
- Manual report preparation is error-prone
- Report submission requires specific formats

**Report Types:**
| Report | Frequency | Due Date |
|--------|-----------|----------|
| VAT Return | Monthly | 25th of next month |
| TDS Return | Monthly | 15th of next month |
| SSF Return | Monthly | 15th of next month |
| CIT Advance Tax | Quarterly | 15th of quarter end |
| CIT Annual Return | Annual | Poush 15 (Dec-Jan) |
| Audit Report | Annual | Ashad 15 (Jul-Aug) |

---

## 4. Nepal Compliance Architecture

### 4.1 Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Nepal Compliance Pack                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │     IRD     │  │     VAT     │  │         PAN         │  │
│  │ Integration │  │  Service    │  │      Service        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │     TDS     │  │     SSF     │  │         CIT         │  │
│  │  Service    │  │  Service    │  │      Service        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    Fiscal   │  │    Nepali   │  │     Invoice         │  │
│  │    Year     │  │   Calendar  │  │      Rules          │  │
│  │  Service    │  │  Service    │  │     Service         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Government Reports Service               │   │
│  └─────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │  Sales  │         │Accounting│         │   HR   │
   │ Service │         │  Service │         │ Service │
   └─────────┘         └──────────┘         └─────────┘
```

### 4.2 Integration Points

| Business Service | Nepal Compliance Integration |
|------------------|------------------------------|
| Sales | VAT calculation, IRD invoice submission, PAN validation |
| Accounting | VAT accounts, TDS entries, CIT calculation |
| Procurement | Vendor PAN, TDS on payments |
| HR | SSF calculation, salary TDS |
| Report | Government report generation |

### 4.3 Configuration Model

```yaml
# Country configuration for Nepal
countryCode: NP
countryName: Nepal
currency: NPR
locale: ne_NP
calendar: bikram_sambat

fiscalYear:
  startMonth: 1  # Baisakh
  startDay: 1
  endMonth: 12   # Chaitra
  endDay: 30/31

tax:
  vat:
    standardRate: 13
    reducedRate: 5
    zeroRate: 0
    exemptCategories: [healthcare, education]
  
  tds:
    rates:
      professional: 15
      rent: 15
      interest: 15
      contractor: 15
    thresholds:
      professional: 50000
      rent: 200000
  
  cit:
    standardRate: 25
    bankingRate: 30
  
  ssf:
    employerRate: 10
    employeeRate: 10
    gratuityRate: 8.33

invoice:
  numberFormat: "NP-{FY}-{SEQUENCE}"
  mandatoryFields:
    - sellerName
    - sellerPAN
    - buyerName
    - buyerPAN
    - invoiceDate
    - items
    - vatAmount
  retentionYears: 10

ird:
  apiEndpoint: "https://ird.gov.np/api"
  environment: "production"  # or "sandbox"
  certificatePath: "/certs/ird.p12"
  timeout: 30000
```

## 5. Compliance Workflows

### 5.1 Invoice Submission Workflow

```
Sales Order Confirmed
    ↓
Generate Invoice
    ↓
Validate Invoice (Nepal rules)
    ↓
Calculate VAT
    ↓
Format for IRD
    ↓
Submit to IRD
    ↓
Store IRD Response
    ↓
Update Invoice Status
```

### 5.2 Tax Return Workflow

```
Period End
    ↓
Collect Tax Data
    ↓
Calculate Tax Liability
    ↓
Generate Return
    ↓
Validate Return
    ↓
Submit to IRD
    ↓
Record Submission
    ↓
Schedule Payment
```

### 5.3 TDS Certificate Workflow

```
Payment Processed
    ↓
Calculate TDS
    ↓
Deduct TDS
    ↓
Generate Certificate
    ↓
Send to Vendor
    ↓
File TDS Return
```

## 6. Extensibility for Other Countries

### 6.1 Country Pack Structure

```
com.erp.platform.compliance.nepal/
├── ird/
│   ├── IrdIntegrationService.java
│   ├── IrdApiClient.java
│   └── IrdCertificateManager.java
├── vat/
│   ├── VatCalculationService.java
│   ├── VatReturnService.java
│   └── VatInvoiceFormatter.java
├── pan/
│   ├── PanValidationService.java
│   └── PanFormatValidator.java
├── tds/
│   ├── TdsCalculationService.java
│   └── TdsCertificateService.java
├── fiscal/
│   ├── FiscalYearService.java
│   └── NepaliCalendarService.java
├── invoice/
│   ├── NepalInvoiceRulesService.java
│   └── InvoiceFormatter.java
└── reports/
    ├── GovernmentReportService.java
    └── ReportFormatters/
```

### 6.2 Adding a New Country

1. Create new country package: `com.erp.platform.compliance.{countryCode}`
2. Implement required services (tax, calendar, invoices)
3. Add country configuration to Country Configuration Service
4. Register country-specific event handlers
5. Add country-specific report templates
6. Test with country-specific scenarios

### 6.3 Country Comparison

| Feature | Nepal | India | USA | UK |
|---------|-------|-------|-----|-----|
| VAT/GST | VAT 13% | GST 18% | Sales Tax | VAT 20% |
| Fiscal Year | Jul-Jun | Apr-Mar | Jan-Dec | Apr-Mar |
| Calendar | Bikram Sambat | Gregorian | Gregorian | Gregorian |
| Tax Authority | IRD | GSTN | IRS | HMRC |
| Invoice Format | IRD mandated | GST mandated | State specific | HMRC mandated |

## 7. Compliance Audit and Monitoring

### 7.1 Audit Requirements

- All tax submissions logged
- All calculations auditable
- Configuration changes tracked
- User actions on compliance features logged

### 7.2 Monitoring

- IRD API availability
- Submission success rate
- Calculation accuracy
- Report generation timing
- Certificate validity

### 7.3 Alerts

- IRD API downtime
- Submission failures
- Certificate expiration
- Report deadline approaching
- Calculation anomalies
