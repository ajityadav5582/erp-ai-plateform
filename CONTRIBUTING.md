# Contributing to ERP AI Platform

Thank you for your interest in contributing to the ERP AI Platform! This document provides guidelines and best practices for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Review Process](#review-process)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

- Java 21 (Temurin/OpenJDK)
- Gradle 8.x (wrapper provided)
- Docker & Docker Compose
- Git
- IDE: IntelliJ IDEA or VS Code with Java extensions

### Setup

```bash
# Clone the repository
git clone https://github.com/erpai/platform.git
cd platform

# Copy environment file
cp infrastructure/docker/.env.example .env

# Start infrastructure
docker compose up -d

# Build the project
./gradlew build

# Run tests
./gradlew test
```

## Development Workflow

### Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch for features
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `hotfix/*` - Production hotfixes
- `release/*` - Release preparation

### Workflow

1. Create a feature branch from `develop`
2. Make your changes following coding standards
3. Write/update tests
4. Run tests and linting locally
5. Commit with conventional commit messages
6. Push and create a pull request to `develop`

## Coding Standards

### Java

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Java 21 features (records, sealed classes, pattern matching)
- Maximum line length: 120 characters
- Use 4 spaces for indentation

### Package Naming

```
com.erpai.platform.<module>.<layer>.<feature>
```

Example:
```
com.erpai.platform.finance.domain.model.Invoice
com.erpai.platform.finance.application.service.InvoiceService
```

### Class Naming

- **Domain**: `Invoice`, `Customer`, `Payment` (no suffix)
- **Service**: `InvoiceService`, `PaymentProcessingService`
- **Repository**: `InvoiceRepository`, `CustomerRepository`
- **Controller**: `InvoiceController`, `ApiController`
- **Configuration**: `PersistenceConfiguration`, `KafkaConfiguration`

### Method Naming

- Use verbs: `calculateTotal()`, `findById()`, `processPayment()`
- Boolean methods: `isActive()`, `hasPermission()`, `canProcess()`

### Testing

- Unit tests: `InvoiceServiceTest`
- Integration tests: `InvoiceRepositoryIT`
- Test classes in same package as source
- Use descriptive test method names: `shouldThrowExceptionWhenInvoiceIsNull()`

## Commit Guidelines

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes
- `build`: Build system changes
- `revert`: Reverting changes

### Examples

```
feat(finance): add invoice generation service
fix(hr): resolve employee lookup by ID
docs(platform): update architecture documentation
refactor(shared): extract common validation logic
```

## Pull Request Process

1. **Title**: Use conventional commit format
2. **Description**: Explain what and why, not how
3. **Tests**: Include tests for new functionality
4. **Documentation**: Update relevant documentation
5. **Self-review**: Review your own changes before requesting review
6. **CI**: Ensure all CI checks pass

### PR Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Tests added/updated
- [ ] All tests pass
- [ ] Documentation updated
- [ ] No merge conflicts
- [ ] Conventional commit message

## Review Process

1. At least one approval required from code owner
2. All CI checks must pass
3. No unresolved conversations
4. Squash and merge to develop

## Questions?

- Documentation: [docs/](docs/)
- Issues: [GitHub Issues](https://github.com/erpai/platform/issues)
- Discussions: [GitHub Discussions](https://github.com/erpai/platform/discussions)
