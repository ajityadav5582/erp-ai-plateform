# Git Branch Strategy

## 1. Purpose

This document defines the Git branching model for the ERP SaaS platform. A consistent branching strategy enables parallel development, clean releases, and efficient collaboration.

## 2. Branching Model: GitFlow with Trunk-Based Development

We use a hybrid approach combining **GitFlow** for release management with **Trunk-Based Development** principles for continuous integration.

## 3. Branch Types

### 3.1 Main Branches

| Branch | Purpose | Protected | Direct Commits |
|--------|---------|-----------|----------------|
| `main` | Production-ready code | Yes | No |
| `develop` | Integration branch for next release | Yes | No |

### 3.2 Supporting Branches

| Branch | Purpose | Lifetime | Created From |
|--------|---------|----------|--------------|
| `feature/*` | New features | Days-Weeks | `develop` |
| `release/*` | Release preparation | Days | `develop` |
| `hotfix/*` | Production fixes | Hours-Days | `main` |
| `bugfix/*` | Bug fixes | Hours-Days | `develop` |
| `chore/*` | Maintenance tasks | Hours-Days | `develop` |

## 4. Branch Naming Conventions

### 4.1 Feature Branches

```
feature/{ticket-id}-{short-description}

# Examples
feature/ERP-123-invoice-creation
feature/ERP-456-payment-processing
feature/ERP-789-customer-search
```

### 4.2 Bugfix Branches

```
bugfix/{ticket-id}-{short-description}

# Examples
bugfix/ERP-101-invoice-total-calculation
bugfix/ERP-102-login-redirect-loop
```

### 4.3 Hotfix Branches

```
hotfix/{ticket-id}-{short-description}

# Examples
hotfix/ERP-201-security-vulnerability
hotfix/ERP-202-payment-gateway-timeout
```

### 4.4 Release Branches

```
release/{version}

# Examples
release/1.2.0
release/2.0.0
```

### 4.5 Chore Branches

```
chore/{description}

# Examples
chore/update-dependencies
chore/upgrade-spring-boot
chore/add-logging-standards
```

## 5. Branch Lifecycle

### 5.1 Feature Branch Flow

```
develop ──► feature/ERP-123 ──► PR ──► develop
                │
                ├── Development work
                ├── CI runs tests
                └── Code review
```

### 5.2 Release Branch Flow

```
develop ──► release/1.2.0 ──► PR ──► main
                │                    │
                ├── Version bump      ├── Tag v1.2.0
                ├── Final testing     └── Deploy to prod
                ├── Bug fixes only
                └── Merge back to develop
```

### 5.3 Hotfix Branch Flow

```
main ──► hotfix/ERP-201 ──► PR ──► main
           │                      │
           ├── Fix production     ├── Tag v1.2.1
           ├── CI runs tests      └── Deploy to prod
           └── Merge back to develop
```

## 6. Branch Protection Rules

### 6.1 Protected Branches

| Branch | Required Reviews | CI Checks | Force Push |
|--------|-----------------|-----------|------------|
| `main` | 2 approvals | All passing | No |
| `develop` | 1 approval | All passing | No |

### 6.2 Required CI Checks

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Code coverage >= 80%
- [ ] SonarQube quality gate passed
- [ ] No high/critical vulnerabilities
- [ ] Checkstyle/SpotBugs pass
- [ ] OpenAPI spec valid (if API changes)

## 7. Workflow

### 7.1 Starting New Work

```bash
# Update local develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/ERP-123-invoice-creation

# Work on feature...
git add .
git commit -m "feat(ERP-123): add invoice creation endpoint"
```

### 7.2 Keeping Feature Branch Updated

```bash
# Fetch latest develop
git fetch origin

# Rebase on develop (preferred)
git rebase origin/develop

# Or merge develop into feature
git merge origin/develop
```

### 7.3 Creating Pull Request

```bash
# Push feature branch
git push origin feature/ERP-123-invoice-creation

# Create PR via GitHub/GitLab UI
# Target: develop
# Title: [ERP-123] Add invoice creation
# Description: See PR template
```

### 7.4 After PR Approval

```bash
# Squash and merge via UI
# Delete feature branch after merge
```

### 7.5 Release Process

```bash
# Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/1.2.0

# Version bump
./gradlew release -PreleaseVersion=1.2.0

# Final testing and bug fixes
# Merge to main
git checkout main
git merge --no-ff release/1.2.0
git tag -a v1.2.0 -m "Release 1.2.0"
git push origin main --tags

# Merge back to develop
git checkout develop
git merge --no-ff release/1.2.0
git push origin develop

# Delete release branch
git branch -d release/1.2.0
```

### 7.6 Hotfix Process

```bash
# Create hotfix branch from main
git checkout main
git pull origin main
git checkout -b hotfix/ERP-201-security-fix

# Fix and test
git add .
git commit -m "fix(ERP-201): patch security vulnerability"

# Merge to main
git checkout main
git merge --no-ff hotfix/ERP-201
git tag -a v1.2.1 -m "Hotfix 1.2.1"
git push origin main --tags

# Merge to develop
git checkout develop
git merge --no-ff hotfix/ERP-201
git push origin develop

# Delete hotfix branch
git branch -d hotfix/ERP-201
```

## 8. Merge Strategies

| Branch Type | Strategy | Reason |
|-------------|----------|--------|
| Feature → develop | Squash and merge | Clean history |
| Release → main | Merge commit | Preserve release history |
| Hotfix → main | Merge commit | Preserve hotfix history |
| Release → develop | Merge commit | Preserve release history |
| Hotfix → develop | Merge commit | Preserve hotfix history |

## 9. Commit Guidelines

See [Commit Message Convention](./14-commit-message-convention.md) for details.

## 10. Branch Cleanup

### 10.1 Automatic Cleanup

- Feature branches: Deleted after PR merge
- Bugfix branches: Deleted after PR merge
- Hotfix branches: Deleted after merge to main and develop
- Release branches: Deleted after release

### 10.2 Manual Cleanup

```bash
# List merged branches
git branch --merged develop | grep -E "feature/|bugfix/"

# Delete merged branches
git branch -d feature/ERP-123-invoice-creation
git branch -d bugfix/ERP-101-fix

# Prune remote branches
git remote prune origin
```

## 11. Branching Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Long-lived feature branches | Merge conflicts, integration issues | Keep branches short-lived (< 1 week) |
| Committing directly to main/develop | Breaks CI, no review | Use PRs for all changes |
| Multiple features in one branch | Hard to review, hard to revert | One feature per branch |
| Not rebasing/merging regularly | Large merge conflicts | Update branch daily |
| Force pushing to shared branches | Loses history, breaks others | Avoid force push to shared branches |

## 12. Git Configuration

### 12.1 Recommended Git Config

```bash
# User configuration
git config --global user.name "Your Name"
git config --global user.email "your.email@company.com"

# Default branch
git config --global init.defaultBranch main

# Pull strategy
git config --global pull.rebase true

# Push behavior
git config --global push.default simple

# Merge tool
git config --global merge.tool vscode

# Diff tool
git config --global diff.tool vscode
```

### 12.2 .gitignore

```
# Build
build/
out/
target/

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Environment
.env
.env.local

# Dependencies
node_modules/

# Logs
*.log
logs/

# Test coverage
coverage/
.jacoco.exec

# Temporary files
*.tmp
*.temp
```

## 13. Git Workflow Checklist

- [ ] Branch created from correct base (develop for features, main for hotfixes)
- [ ] Branch name follows naming convention
- [ ] CI passes on feature branch
- [ ] PR created with proper description
- [ ] Code review completed
- [ ] All CI checks pass
- [ ] Branch deleted after merge
- [ ] Hotfix merged to both main and develop
