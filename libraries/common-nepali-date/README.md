# common-nepali-date

Reusable Bikram Sambat (BS / Nepali) date support for the ERP AI Platform.

One module owns all Nepali calendar arithmetic. Everything else — identity, inventory,
procurement, finance, sales, HR, and the frontend — reads from here.

---

## The one rule: AD is canonical, BS is derived

**Store and query Gregorian dates. Derive Bikram Sambat for display and Nepali
statutory reporting. Never index, sort or filter on the BS columns.**

This matters because BS conversion is a *projection*, not a fact. The Nepali calendar
committee revises published month lengths, so any BS column can become stale. The
Gregorian date never does.

| Concern | Use |
|---|---|
| Persistence, ranges, sorting, indexes, `BETWEEN` queries | `<field>_ad` |
| Display, Nepali-language UI, statutory documents | `<field>_bs` |
| "which BS year/month is this" filters (reports only) | `<field>_bs_year`, `<field>_bs_month` |

---

## Why this module exists

The platform previously carried a hand-written BS month table inside
`com.erp.platform.identity.domain.BsCalendarData`. **It was wrong.** It duplicated
month 12 as 31 days for every year, which made the calendar drift further the further
you got from its single anchor:

```
AD 2024-04-13  ->  BS 2081-01-02   (should be 2081-01-01)
AD 2023-04-14  ->  BS 2080-01-03   (should be 2080-01-01)
AD 2022-04-14  ->  BS 2079-01-04   (should be 2079-01-01)
```

That table had already been used to seed all **7683** rows of `master_bs_calendar`,
where every Bikram Sambat New Year was off by **-7 to +5 days**. It also caused
`FY 2081/82` and `FY 2082/83` to carry `end_date_ad` one day past their real boundary,
which would have pulled a day of the next fiscal year into year-end reports.

This module replaces that table with one validated against every published New Year
anchor for BS 2070–2084.

---

## Calendar accuracy

### Validated anchors

The table reproduces all published Bikram Sambat New Year dates exactly:

| BS | AD | BS | AD |
|---|---|---|---|
| 2070-01-01 | 2013-04-14 | 2078-01-01 | 2021-04-14 |
| 2071-01-01 | 2014-04-14 | 2079-01-01 | 2022-04-14 |
| 2072-01-01 | 2015-04-14 | 2080-01-01 | 2023-04-14 |
| 2073-01-01 | 2016-04-13 | 2081-01-01 | 2024-04-13 |
| 2074-01-01 | 2017-04-14 | 2082-01-01 | 2025-04-14 |
| 2075-01-01 | 2018-04-14 | 2083-01-01 | 2026-04-14 |
| 2076-01-01 | 2019-04-14 | 2084-01-01 | 2027-04-14 |
| 2077-01-01 | 2020-04-13 | | |

Known national dates also check out: Falgun 7 2080 = 2024-02-19 (Democracy Day),
Asoj 3 2072 = 2015-09-20 (Constitution Day), Jestha 15 2081 = 2024-05-28 (Republic Day).

### Supported range and the provisional warning

- Supported: **BS 2000–2090** (AD 1943-04-14 – 2034-04-13).
- **Published and trustworthy: BS 2000–2084.**
- **BS 2085+ is provisional.** The committee publishes month lengths year by year, so
  later years are projections. `BsCalendarData.PROVISIONAL_FROM_YEAR` exposes this and
  `NepaliDateDetails.provisional()` flags affected rows.

> **Do not use BS 2085+ for tax filing, VAT, or statutory fiscal reporting** until the
> month lengths are re-verified against an official `ncb.gov.np` publication. Update
> `MONTH_DAYS` and regenerate the seed when new data is published.

### Round-trip guarantee

Every day from BS 2000-01-01 to BS 2090-12-last satisfies
`toAd(fromAd(d)) == d`. BS 2070–2090 is exactly **7670** days (the corrupt table held
7683 — 13 spurious days).

---

## Usage

### Convert

```java
import com.erp.platform.common.nepali.date.*;

// AD -> BS
NepaliDate bs = NepaliDateConverter.fromAd(LocalDate.of(2024, 4, 13));
// bs = 2081-01-01

// BS -> AD
LocalDate ad = NepaliDateConverter.toAd(2081, 1, 1);   // 2024-04-13
LocalDate ad2 = NepaliDateConverter.toAd("2081-01-01"); // 2024-04-13

// Text
String text = NepaliDateConverter.format(LocalDate.of(2024, 4, 13)); // "2081-01-01"
String longForm = NepaliDateConverter.formatLong(LocalDate.of(2024, 4, 13));
// "2081 Baisakh 01 (Saturday)"
```

### Full details for API responses

Return `NepaliDateDetails` from controllers so the frontend never runs its own calendar
arithmetic:

```java
NepaliDateDetails d = NepaliDateConverter.details(LocalDate.of(2024, 4, 13));

d.bsDate();          // "2081-01-01"
d.bsDateNp();        // "२०८१-०१-०१"   Devanagari digits
d.monthNameEn();     // "Baisakh"
d.monthNameNp();     // "बैशाख"
d.dayNameEn();       // "Saturday"
d.dayNameNp();       // "शनिबार"
d.weekend();         // true
d.provisional();     // false
```

### Entity columns — use the embeddable

`DualDateColumns` stores AD and BS side by side and **cannot drift**. Every BS column is
recomputed from AD on `@PrePersist` and `@PreUpdate`, so legacy rows and direct field
mutation are repaired automatically rather than persisted.

```java
@Entity
public class PurchaseOrder extends TenantScopedEntity<Long> {

    @Embedded(prefix = "issue_")
    private DualDateColumns issueDate = DualDateColumns.of(LocalDate.now());

    @Embedded(prefix = "due_")
    private DualDateColumns dueDate;

    public void setDueDate(LocalDate due) {
        this.dueDate = DualDateColumns.of(due);
    }
}
```

Generated columns, with `prefix = "issue_"`:

```
issue_ad              date          canonical
issue_bs              varchar(10)   derived
issue_bs_year         integer       derived
issue_bs_month        integer       derived
issue_bs_day          integer       derived
issue_bs_provisional  boolean       derived
```

Users may type either calendar:

```java
DualDateColumns.of(LocalDate.of(2024, 4, 13)); // enter AD
DualDateColumns.ofBs("2081-01-01");            // enter BS
DualDateColumns.ofBs(NepaliDate.of(2081, 1, 1));
```

`isConsistent()` exposes drift for diagnostics and tests.

### Add the dependency

```gradle
dependencies {
    implementation project(':libraries:common-nepali-date')
}
```

---

## Validation rules

- BS year must be within 2000–2090.
- BS month must be 1–12.
- BS day must be 1..`daysInMonth(year, month)`. Month lengths are genuinely irregular —
  Jestha 2081 has 32 days, Kartik 2081 has 30 — so never assume 30.
- Nulls are rejected, never silently defaulted.
- Malformed text raises `NepaliDateException` with a message naming the input.

---

## Database repair

Two scripts repair data generated by the old, corrupt table:

- `infrastructure/postgres/flyway/platform/R001__repair_master_bs_calendar.sql`
  Reseeds all 7670 days of BS 2070–2090. **Generated from this module**, so code and
  database cannot disagree.
- `infrastructure/postgres/flyway/platform/R002__repair_master_fiscal_years.sql`
  Realigns `master_fiscal_years` AD dates against their BS strings and restores the
  calendar-to-fiscal-year foreign key.

Apply:

```bash
docker exec -i erpai-postgres psql -U erpai -d erpai_platform \
  -v ON_ERROR_STOP=1 < infrastructure/postgres/flyway/platform/R001__repair_master_bs_calendar.sql
docker exec -i erpai-postgres psql -U erpai -d erpai_platform \
  -v ON_ERROR_STOP=1 < infrastructure/postgres/flyway/platform/R002__repair_master_fiscal_years.sql
```

Note the `-i`: without it the heredoc never reaches the container and `psql` exits 0
having done nothing.

`is_holiday` is reset to `false` by the reseed — holiday marking is operational data owned
by the calendar admin, not calendar arithmetic. Re-apply it after migrating.

---

## Regenerating the seed

The seed is produced by running the library, so it can never drift from the code:

```java
for (int y = 2070; y <= 2090; y++) {
    for (NepaliDateDetails d : NepaliDateConverter.expandBsYear(y)) {
        // emit INSERT row from d.adDate(), d.bsDate(), d.monthNameNp(), ...
    }
}
```

---

## Tests

```bash
./gradlew :libraries:common-nepali-date:test
```

Covers published New Year anchors, full-range round trips, national dates, year
boundaries, month-length sanity (365/366 days, 29–32 per month), weekday index
alignment, weekend flags, Devanagari digit conversion, exact Unicode for every Nepali
month and weekday name, out-of-range and malformed input, and JPA drift repair.

---

## Migration notes for other modules

1. Replace hand-rolled date logic with `NepaliDateConverter`.
2. Replace loose `xxxAd` / `xxxBs` field pairs with `@Embedded DualDateColumns`.
3. Query on the `_ad` column.
4. Delete any local copy of a BS month table — there must be exactly one.
5. If a module keeps its own `BsCalendarData`, replace it with a delegating shim, as
   `platform/identity` now does.
