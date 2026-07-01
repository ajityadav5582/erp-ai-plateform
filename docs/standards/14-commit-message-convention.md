# Commit Message Convention

## 1. Purpose

This document defines the commit message format for the ERP SaaS platform. Consistent commit messages enable automated changelog generation, semantic versioning, and clear project history.

## 2. Format: Conventional Commits

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification with customizations for our ERP platform.

### 2.1 Basic Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### 2.2 Example

```
feat(finance): add invoice creation endpoint

- Implement POST /api/v1/invoices
- Add validation for invoice line items
- Include tenant isolation

Closes ERP-123
```

## 3. Commit Types

| Type | Description | Version Bump |
|------|-------------|--------------|
| `feat` | New feature | MINOR |
| `fix` | Bug fix | PATCH |
| `docs` | Documentation only | - |
| `style` | Code style (formatting, missing semicolons) | - |
| `refactor` | Code change that neither fixes bug nor adds feature | - |
| `perf` | Performance improvement | PATCH |
| `test` | Adding or updating tests | - |
| `chore` | Maintenance tasks (dependencies, build) | - |
| `ci` | CI/CD changes | - |
| `build` | Build system changes | - |
| `revert` | Revert previous commit | - |

## 4. Commit Scopes

Scopes indicate the area of the codebase affected by the change.

### 4.1 Domain Scopes

| Scope | Description |
|-------|-------------|
| `finance` | Finance module (invoices, payments, accounting) |
| `hr` | HR module (employees, payroll, attendance) |
| `inventory` | Inventory module (products, stock, warehouses) |
| `sales` | Sales module (orders, customers, quotes) |
| `procurement` | Procurement module (purchases, vendors) |
| `manufacturing` | Manufacturing module (BOM, production) |

### 4.2 Technical Scopes

| Scope | Description |
|-------|-------------|
| `api` | REST API changes |
| `auth` | Authentication/authorization |
| `db` | Database changes (migrations, schema) |
| `events` | Event/messaging changes |
| `security` | Security-related changes |
| `tenancy` | Multi-tenancy changes |
| `logging` | Logging changes |
| `config` | Configuration changes |
| `deps` | Dependency updates |
| `docker` | Docker/container changes |
| `k8s` | Kubernetes changes |

### 4.3 Cross-Cutting Scopes

| Scope | Description |
|-------|-------------|
| `core` | Platform core changes |
| `shared` | Shared library changes |
| `infra` | Infrastructure changes |

## 5. Commit Message Rules

### 5.1 Subject Line

- **Length:** Maximum 50 characters
- **Case:** Lowercase (except proper nouns, acronyms)
- **Punctuation:** No period at end
- **Tense:** Imperative mood ("add" not "added" or "adds")

### 5.2 Body

- **Length:** Maximum 72 characters per line
- **Separator:** Blank line between subject and body
- **Content:** Explain WHAT and WHY, not HOW
- **Tense:** Present tense

### 5.3 Footer

- **Breaking changes:** `BREAKING CHANGE: <description>`
- **Issue references:** `Closes ERP-123`, `Refs ERP-456`
- **Co-authors:** `Co-authored-by: Name <email>`

## 6. Examples

### 6.1 Feature Commit

```
feat(finance): add invoice PDF generation

Implement PDF generation for invoices using Apache PDFBox.
PDFs are stored in MinIO and accessible via download link.

Closes ERP-123
```

### 6.2 Bug Fix Commit

```
fix(inventory): prevent negative stock on hand

Stock on hand was going negative due to race condition in
concurrent stock adjustments. Added pessimistic locking.

Fixes ERP-456
```

### 6.3 Documentation Commit

```
docs(api): update invoice API documentation

Add examples for invoice creation and retrieval.
Document error responses.
```

### 6.4 Refactor Commit

```
refactor(finance): extract payment calculation to domain service

Move payment calculation logic from InvoiceService to
PaymentDomainService for better separation of concerns.
```

### 6.5 Breaking Change Commit

```
feat(api)!: change invoice status enum values

BREAKING CHANGE: InvoiceStatus values changed from
PENDING/PAID/CANCELLED to CREATED/APPROVED/PAID/CANCELLED/VOID.

Migration guide: Update status checks in client code.
```

### 6.6 Revert Commit

```
revert: feat(finance): add invoice PDF generation

This reverts commit abc123def456.

Reason: PDF generation causing memory issues in production.
Will revisit with streaming approach.
```

## 7. Commit Best Practices

### 7.1 Atomic Commits

- **One logical change per commit**
- **Don't mix refactoring with features**
- **Don't mix formatting with logic changes**

### 7.2 Commit Often

- **Small, frequent commits** over large, infrequent ones
- **Commit at logical checkpoints**
- **Don't wait until end of day**

### 7.3 Write Good Messages

- **Be specific:** "Fix null pointer" → "fix(auth): handle null token in JWT filter"
- **Explain why:** Not just what changed
- **Reference issues:** Link to ticket/issue

### 7.4 Don't Commit

- Secrets, credentials, API keys
- Large binary files (use Git LFS or artifact storage)
- Generated files (unless necessary)
- IDE configuration files
- Log files

## 8. Commit Message Validation

### 8.1 Commitlint Configuration

```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'style', 'refactor', 'perf', 'test', 'chore', 'ci', 'build', 'revert'
    ]],
    'subject-case': [2, 'never', ['start-case', 'pascal-case', 'upper-case']],
    'subject-max-length': [2, 'always', 50],
    'body-max-line-length': [2, 'always', 72],
    'header-max-length': [2, 'always', 72]
  }
};
```

### 8.2 Husky Integration

```json
{
  "husky": {
    "hooks": {
      "commit-msg": "commitlint -E HUSKY_GIT_PARAMS",
      "pre-commit": "lint-staged"
    }
  },
  "lint-staged": {
    "*.java": ["checkstyle", "spotbugs", "git add"]
  }
}
```

## 9. Semantic Versioning

### 9.1 Version Format

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
1.2.3
1.2.3-alpha.1
1.2.3+build.123
```

### 9.2 Version Bump Rules

| Change | Version Bump | Example |
|--------|-------------|---------|
| Breaking change | MAJOR | 1.0.0 → 2.0.0 |
| New feature | MINOR | 1.0.0 → 1.1.0 |
| Bug fix | PATCH | 1.0.0 → 1.0.1 |

### 9.3 Automated Versioning

Use `semantic-release` or similar tool:

```yaml
# .github/workflows/release.yml
- name: Release
  uses: cycjimmy/semantic-release-action@v3
  with:
    semantic_version: 19.0.5
    extra_plugins: |
      @semantic-release/changelog
      @semantic-release/git
```

## 10. Changelog Generation

### 10.1 Format

```markdown
# Changelog

## [1.2.0] - 2024-01-15

### Features
- feat(finance): add invoice PDF generation (ERP-123)
- feat(inventory): add barcode scanning support (ERP-124)

### Bug Fixes
- fix(inventory): prevent negative stock on hand (ERP-456)
- fix(auth): handle null token in JWT filter (ERP-457)

### Documentation
- docs(api): update invoice API documentation

### Dependency Updates
- chore(deps): upgrade Spring Boot to 3.2.0
```

### 10.2 Automated Generation

Use `semantic-release` with conventional changelog:

```json
{
  "plugins": [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    "@semantic-release/changelog",
    "@semantic-release/npm",
    "@semantic-release/git",
    "@semantic-release/github"
  ]
}
```

## 11. Commit Message Checklist

- [ ] Type is valid (feat, fix, docs, etc.)
- [ ] Scope is appropriate (if used)
- [ ] Subject is <= 50 characters
- [ ] Subject is lowercase (except proper nouns)
- [ ] Subject is imperative mood
- [ ] Body explains what and why (if needed)
- [ ] Issue reference included (if applicable)
- [ ] Breaking change noted (if applicable)
- [ ] No secrets or sensitive data
