# Pull Request Checklist

## 1. Purpose

This checklist ensures all pull requests meet quality, security, and maintainability standards before merging. All PRs must pass all applicable checks.

## 2. PR Creation

### 2.1 Before Creating PR

- [ ] Code compiles without errors
- [ ] All tests pass locally
- [ ] Code follows coding standards
- [ ] Self-review completed
- [ ] Commit messages follow convention
- [ ] No secrets or credentials committed
- [ ] No debug code or console.log statements

### 2.2 PR Description Template

```markdown
## Description
<!-- Brief description of changes -->

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing functionality to change)
- [ ] Documentation update
- [ ] Refactoring (no functional changes)
- [ ] Performance improvement
- [ ] Test update

## Related Issues
<!-- Link to related issues -->
Closes ERP-123
Related to ERP-456

## Changes Made
<!-- Bullet list of specific changes -->
- Added invoice creation endpoint
- Added validation for invoice line items
- Updated database schema

## Testing
<!-- Describe testing performed -->
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Test coverage >= 80%

## Screenshots (if applicable)
<!-- Add screenshots for UI changes -->

## Checklist
- [ ] Code follows coding standards
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass
- [ ] Security considerations addressed
- [ ] Performance considerations addressed
- [ ] Multi-tenant isolation maintained
```

## 3. Code Review Checklist

### 3.1 Functionality

- [ ] Code does what it's supposed to do
- [ ] Edge cases handled
- [ ] Error handling is appropriate
- [ ] No obvious bugs
- [ ] Logic is correct

### 3.2 Design

- [ ] Follows SOLID principles
- [ ] Single responsibility maintained
- [ ] Appropriate design patterns used
- [ ] No code duplication
- [ ] Abstractions are appropriate (not over/under-engineered)

### 3.3 Naming

- [ ] Variable names are descriptive
- [ ] Method names indicate action
- [ ] Class names are nouns
- [ ] Constants are UPPER_SNAKE_CASE
- [ ] No abbreviations unless well-known

### 3.4 Tests

- [ ] Unit tests cover new code
- [ ] Tests are meaningful (not just coverage)
- [ ] Test names are descriptive
- [ ] Edge cases tested
- [ ] No flaky tests
- [ ] Mocks are appropriate (not over-mocked)

### 3.5 Security

- [ ] No SQL injection vulnerabilities
- [ ] No XSS vulnerabilities
- [ ] Input validation present
- [ ] No secrets in code
- [ ] Authentication/authorization correct
- [ ] Sensitive data not logged
- [ ] Error messages don't leak information

### 3.6 Performance

- [ ] No N+1 queries
- [ ] Appropriate indexes used
- [ ] No memory leaks
- [ ] Efficient algorithms used
- [ ] Pagination for large datasets
- [ ] Caching where appropriate

### 3.7 Multi-Tenancy

- [ ] Tenant isolation maintained
- [ ] All queries filtered by tenant
- [ ] No cross-tenant data access
- [ ] Tenant context properly set

### 3.8 Documentation

- [ ] Javadoc for public APIs
- [ ] Complex logic commented
- [ ] README updated if needed
- [ ] API documentation updated
- [ ] ADR created if architectural change

## 4. Automated Checks

### 4.1 CI Pipeline Checks

| Check | Tool | Pass Criteria |
|-------|------|---------------|
| Compilation | Gradle | Build successful |
| Unit Tests | JUnit 5 | All pass, coverage >= 80% |
| Integration Tests | Testcontainers | All pass |
| Static Analysis | SpotBugs | No errors |
| Code Style | Checkstyle | No violations |
| Dependency Check | OWASP | No high/critical CVEs |
| SonarQube | SonarQube | Quality gate passed |
| OpenAPI | Springdoc | Spec valid |

### 4.2 Security Scans

| Scan | Tool | Pass Criteria |
|------|------|---------------|
| SAST | SonarQube | No new vulnerabilities |
| Dependency Check | OWASP DC | No high/critical CVEs |
| Secret Detection | GitGuardian | No secrets found |
| Container Scan | Trivy | No high/critical CVEs |

## 5. Review Process

### 5.1 Review Assignment

- **Author:** Assigns reviewers based on code area
- **Minimum reviewers:** 1 for bug fixes, 2 for features
- **Domain expert:** At least one reviewer familiar with the domain
- **Security review:** Required for auth, data access, external integrations

### 5.2 Review Timeline

| Priority | Review Time |
|----------|-------------|
| Hotfix | 2 hours |
| Bug fix | 4 hours |
| Feature | 1 business day |
| Documentation | 2 business days |

### 5.3 Review Comments

- **Be constructive:** Focus on code, not person
- **Be specific:** Point to exact lines
- **Explain why:** Don't just say "change this"
- **Suggest alternatives:** Provide solutions, not just problems
- **Distinguish:** Must-fix vs. nice-to-have

### 5.4 Review Resolution

- **Author:** Addresses all comments
- **Reviewer:** Approves when satisfied
- **Disagreements:** Escalate to tech lead/architect
- **Blocking issues:** Must be resolved before merge

## 6. Merge Requirements

### 6.1 Before Merge

- [ ] All CI checks pass
- [ ] All review comments addressed
- [ ] At least required approvals obtained
- [ ] No merge conflicts
- [ ] Branch is up-to-date with target
- [ ] Tests added/updated
- [ ] Documentation updated

### 6.2 Merge Strategy

| PR Type | Strategy |
|---------|----------|
| Feature | Squash and merge |
| Bugfix | Squash and merge |
| Hotfix | Merge commit (preserve history) |
| Release | Merge commit (preserve history) |

### 6.3 After Merge

- [ ] Feature branch deleted
- [ ] Ticket updated (moved to Done/Deployed)
- [ ] Release notes updated (if applicable)
- [ ] Deployment scheduled (if applicable)

## 7. PR Size Guidelines

| Size | Lines Changed | Review Time |
|------|--------------|-------------|
| XS | 1-50 | 15 minutes |
| S | 51-200 | 30 minutes |
| M | 201-500 | 1 hour |
| L | 501-1000 | 2 hours |
| XL | 1000+ | Split into multiple PRs |

### 7.1 Large PR Handling

If PR exceeds 500 lines:
- [ ] Split into logical, independent changes
- [ ] Create separate PRs for each change
- [ ] Or create feature flag for incremental rollout

## 8. Common Review Findings

### 8.1 Critical (Must Fix)

- Security vulnerabilities
- Data loss risks
- Breaking changes without migration
- Missing error handling
- Incorrect business logic

### 8.2 Major (Should Fix)

- Missing tests
- Performance issues
- Code duplication
- Poor naming
- Missing documentation

### 8.3 Minor (Nice to Have)

- Code style improvements
- Refactoring suggestions
- Alternative implementations
- Additional test cases

## 9. PR Templates

### 9.1 Feature PR Template

```markdown
## Description
<!-- What does this PR do? -->

## Motivation
<!-- Why is this change needed? -->

## Implementation
<!-- How was this implemented? -->

## Testing
<!-- How was this tested? -->

## Screenshots
<!-- If applicable -->

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Performance considered
- [ ] Security reviewed
```

### 9.2 Bugfix PR Template

```markdown
## Description
<!-- What bug does this fix? -->

## Root Cause
<!-- What caused the bug? -->

## Fix
<!-- How does this fix the bug? -->

## Testing
<!-- How was this verified? -->

## Regression Risk
<!-- What could this break? -->

## Checklist
- [ ] Root cause identified
- [ ] Fix is minimal
- [ ] Tests added to prevent regression
- [ ] No side effects
```

### 9.3 Hotfix PR Template

```markdown
## Description
<!-- What is the production issue? -->

## Impact
<!-- Who is affected? How severe? -->

## Fix
<!-- What is the fix? -->

## Testing
<!-- How was this tested? -->

## Rollout Plan
<!-- How will this be deployed? -->

## Checklist
- [ ] Fix is minimal and targeted
- [ ] Tests pass
- [ ] Security reviewed
- [ ] Deployment plan ready
```

## 10. Review Etiquette

### 10.1 For Authors

- **Be responsive:** Address comments promptly
- **Be humble:** Accept feedback gracefully
- **Explain:** Provide context for decisions
- **Iterate:** Update PR based on feedback
- **Appreciate:** Thank reviewers for their time

### 10.2 For Reviewers

- **Be timely:** Review within agreed timeframe
- **Be thorough:** Don't approve without reading
- **Be kind:** Critique code, not people
- **Be clear:** Explain concerns clearly
- **Be helpful:** Suggest improvements

## 11. PR Metrics

Track these metrics to improve the process:

- **PR cycle time:** Time from creation to merge
- **Review time:** Time from PR creation to first review
- **Rework cycles:** Number of review rounds
- **PR size:** Lines changed per PR
- **Approval rate:** Percentage of PRs approved first time

## 12. Emergency PR Process

For critical production issues:

1. **Create hotfix branch** from main
2. **Minimal fix** - only what's needed
3. **Fast-track review** - 1 reviewer, 2 hour SLA
4. **Deploy immediately** after merge
5. **Post-mortem** after resolution
