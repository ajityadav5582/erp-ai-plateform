# Development Guide

## Overview

This directory contains development guides and best practices.

## Documents

- [Getting Started](getting-started.md) - Development environment setup
- [Coding Standards](coding-standards.md) - Code style and conventions
- [Testing Guide](testing.md) - Testing strategies and practices
- [Debugging](debugging.md) - Debugging techniques
- [Performance](performance.md) - Performance optimization

## Quick Start

```bash
# Clone and setup
git clone https://github.com/erpai/platform.git
cd platform
cp infrastructure/docker/.env.example .env

# Start infrastructure
docker compose up -d

# Build and test
./gradlew build

# Run a specific service
./gradlew :platform:core:interfaces:bootRun
```
