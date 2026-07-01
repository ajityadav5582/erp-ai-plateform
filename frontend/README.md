# ERP AI Platform - Frontend

## Overview

This directory contains the frontend applications for the ERP AI Platform.

## Structure

```
frontend/
├── web/                    # Web application (React/TypeScript)
│   ├── app/               # Next.js app directory
│   ├── components/        # Reusable UI components
│   ├── hooks/             # Custom React hooks
│   ├── lib/               # Utility libraries
│   ├── public/            # Static assets
│   └── styles/            # Global styles
├── mobile/                 # Mobile application (React Native)
│   ├── app/               # App screens and navigation
│   ├── components/        # Mobile UI components
│   ├── hooks/             # Custom hooks
│   └── lib/               # Utility libraries
└── shared/                 # Shared code between web and mobile
    ├── components/        # Shared components
    ├── hooks/             # Shared hooks
    ├── lib/               # Shared utilities
    ├── types/             # TypeScript type definitions
    └── utils/             # Shared utility functions
```

## Technology Stack

### Web
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript 5.x
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **Data Fetching**: TanStack Query
- **Forms**: React Hook Form + Zod
- **Testing**: Jest + React Testing Library + Playwright

### Mobile
- **Framework**: React Native
- **Language**: TypeScript 5.x
- **Navigation**: React Navigation
- **State Management**: Zustand
- **Testing**: Jest + React Native Testing Library

## Development

```bash
# Install dependencies
cd frontend/web
npm install

# Start development server
npm run dev

# Run tests
npm run test

# Build for production
npm run build
```

## Architecture

- Feature-based folder structure
- Atomic Design principles for components
- API-first integration with backend
- Responsive design (mobile-first)
- Accessibility (WCAG 2.1 AA)
