# Frontend Authentication Documentation

## Overview

This document describes the frontend authentication system for the ERP AI Platform. It covers the technologies used, their purposes, and the complete login flow from user interaction to backend communication.

---

## Table of Contents

1. [Technology Stack](#technology-stack)
2. [Architecture Overview](#architecture-overview)
3. [Login Process Flow](#login-process-flow)
4. [Error Handling](#error-handling)
5. [Session Management](#session-management)
6. [File Reference](#file-reference)

---

## Technology Stack

### 1. Next.js 14 (App Router)

**What it is:** A React framework for building full-stack web applications with server-side rendering, static site generation, and client-side navigation.

**Why we use it:**
- Provides file-system based routing with the App Router (`app/` directory)
- Supports React Server Components (RSC) and Client Components (`"use client"` directive)
- Built-in optimizations for images, fonts, and scripts
- API routes for backend functionality (though we use a separate Spring Boot backend)

**How it works in our auth system:**
- The login page lives at `frontend/src/app/login/page.tsx`
- Protected routes use the `(protected)` route group with `ProtectedRoute` component
- Client Components (`"use client"`) handle interactive forms and state management

### 2. React 18

**What it is:** A JavaScript library for building user interfaces with component-based architecture.

**Why we use it:**
- Declarative UI paradigm
- Component reusability
- Hooks for state and side-effect management (`useState`, `useEffect`, `useCallback`, `useMemo`)

**How it works in our auth system:**
- `LoginForm` is a React component that manages form state with `useState`
- `AuthProvider` uses React Context to share auth state across the app
- `useAuth` hook provides a clean API for components to access auth functions

### 3. Redux Toolkit + RTK Query

**What it is:** Redux Toolkit is the official, opinionated toolset for efficient Redux development. RTK Query is a powerful data fetching and caching tool built on top of Redux Toolkit.

**Why we use it:**
- Centralized state management for authentication
- RTK Query provides automatic caching, deduplication, and refetching
- Reduces boilerplate compared to vanilla Redux
- Built-in loading/error states for API calls

**How it works in our auth system:**
- `authApi` in `auth.service.ts` defines endpoints: `login`, `logout`, `refreshToken`, `getCurrentUser`
- `useLoginMutation()` hook provides `loginMutation` function and loading state
- The auth slice (`auth.slice.ts`) stores `user`, `accessToken`, `refreshToken`, `tenantId`, `status`
- `createAsyncThunk` handles async operations like `refreshTokenThunk` and `fetchCurrentUserThunk`

### 4. Axios

**What it is:** A promise-based HTTP client for the browser and Node.js.

**Why we use it:**
- Request/response interceptors for automatic token injection
- Automatic JSON transformation
- Better error handling than native `fetch`
- Timeout configuration

**How it works in our auth system:**
- `axiosInstance` in `http-client.ts` is the shared Axios instance
- Base URL is configured from environment variables (`API_BASE_URL`)
- Request interceptor injects `Authorization: Bearer {token}` and `X-Tenant-ID` headers
- Response interceptor handles 401 errors by attempting token refresh

### 5. Zod

**What it is:** A TypeScript-first schema declaration and validation library.

**Why we use it:**
- Runtime type validation for form inputs
- Composable schemas with helpful error messages
- Type inference from schemas (`z.infer<typeof schema>`)
- Integration with React Hook Form via `@hookform/resolvers`

**How it works in our auth system:**
- `loginSchema` in `login-form.tsx` validates `tenantId`, `username`, `password`, `rememberMe`
- `z.coerce.number()` converts string input to number for `tenantId`
- `z.string().min(1).max(255)` enforces length constraints matching backend validation
- `zodResolver(loginSchema)` connects Zod to React Hook Form

### 6. React Hook Form

**What it is:** A performant, flexible, and extensible form library for React.

**Why we use it:**
- Minimizes re-renders compared to controlled components
- Built-in validation integration
- Simple API for form state management
- Works well with Zod via resolvers

**How it works in our auth system:**
- `useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })` creates the form instance
- `form.handleSubmit(onSubmit)` wraps the submit handler with validation
- `FormField` components connect form state to UI inputs
- `form.setError()` can be used for server-side validation errors

### 7. Sonner (Toast Notifications)

**What it is:** A minimal, customizable toast notification library for React.

**Why we use it:**
- Non-intrusive feedback for user actions
- Supports success, error, and info variants
- Auto-dismiss with configurable duration
- Positioned in the top-right corner

**How it works in our auth system:**
- `toast.success("Welcome back, {name}!")` on successful login
- `toast.error("Sign in failed", { description: message })` on login failure
- `toast.error("Account locked", { description: message, duration: 6000 })` for account lockout
- Configured in `providers.tsx` with `position="top-right"` and `duration={4000}`

### 8. Tailwind CSS + shadcn/ui

**What it is:** Tailwind CSS is a utility-first CSS framework. shadcn/ui is a collection of re-usable components built with Radix UI and Tailwind CSS.

**Why we use it:**
- Rapid UI development with utility classes
- Consistent design system via shadcn/ui components
- Dark mode support
- Accessible components (Radix UI primitives)

**How it works in our auth system:**
- `Button`, `Input`, `Form`, `Checkbox` components from shadcn/ui
- `FormMessage` displays validation errors
- `Loader2` icon from Lucide React shows loading state
- Responsive design with Tailwind breakpoints (`sm:`, `md:`, `lg:`)

### 9. Lucide React

**What it is:** A library of beautiful, consistent icons for React applications.

**Why we use it:**
- Tree-shakeable (only import icons you use)
- Consistent design language
- Small bundle size

**How it works in our auth system:**
- `Eye` / `EyeOff` icons for password visibility toggle
- `Loader2` icon for loading spinner during form submission
- `AlertTriangle` icon for account-locked warning state

### 10. JWT (JSON Web Tokens)

**What it is:** A compact, URL-safe means of representing claims to be transferred between two parties.

**Why we use it:**
- Stateless authentication (no server-side session storage)
- Contains user claims (userId, tenantId, roles, etc.)
- Short-lived access tokens + long-lived refresh tokens for security

**How it works in our auth system:**
- Backend issues `accessToken` (JWT, 15 min) and `refreshToken` (opaque, 7 days)
- Access token stored in memory + cookies
- Refresh token stored in cookies
- Axios interceptor automatically attaches `Authorization: Bearer {accessToken}` header
- On 401, interceptor calls `/auth/refresh` to get a new token pair

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser / Client                          │
│                                                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │  LoginPage  │───▶│  LoginForm  │───▶│   AuthProvider      │  │
│  │  (page.tsx) │    │ (component) │    │   (Context)         │  │
│  └─────────────┘    └─────────────┘    └──────────┬──────────┘  │
│                                                    │              │
│  ┌─────────────┐    ┌─────────────┐    ┌──────────▼──────────┐  │
│  │ ForgotPass  │    │  authApi    │    │   Auth Slice        │  │
│  │ (page.tsx)  │    │ (RTK Query) │    │   (Redux)           │  │
│  └─────────────┘    └──────┬──────┘    └─────────────────────┘  │
│                            │                                     │
│  ┌─────────────────────────▼─────────────────────────────────┐   │
│  │              axiosInstance (Axios)                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────────────┐  │   │
│  │  │  Request    │  │  Response   │  │  Token Store      │  │   │
│  │  │ Interceptor │  │ Interceptor │  │  (in-memory)      │  │   │
│  │  └─────────────┘  └─────────────┘  └───────────────────┘  │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                         │
│                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │  AuthController │───▶│  AuthServiceImpl│───▶│   UserRepo  │ │
│  │  (/api/v1/identity/auth) │    │                 │    │             │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
│                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │  JwtService     │    │  RefreshToken   │                     │
│  │                 │    │  Repository     │                     │
│  └─────────────────┘    └─────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Login Process Flow

### Step-by-Step Login Flow

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  1.     │────▶│  2.     │────▶│  3.     │────▶│  4.     │
│  User   │     │  Form   │     │  Auth   │     │  Axios  │
│  enters │     │  submit │     │ Provider│     │ Request │
│  creds  │     │  valid? │     │  login()│     │  sent   │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
                                                      │
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  9.     │◀────│  8.     │◀────│  7.     │◀────│  6.     │
│  Redirect│     │  Toast  │     │  Store  │     │  Backend│
│  to /   │     │  success│     │  update │     │  resp   │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
       ▲                                           │
       │              ┌─────────┐     ┌─────────┐   │
       │              │  5.     │     │  Error  │   │
       └──────────────│  Error  │────▶│  handle │◀──┘
                      │  path   │     │         │
                      └─────────┘     └─────────┘
```

### Detailed Step Description

#### Step 1: User Enters Credentials
- User fills in `tenantId`, `username/email`, `password`, and optionally checks "Remember me"
- React Hook Form validates input in real-time using Zod schema
- Validation rules:
  - `tenantId`: required, must be a positive integer
  - `username`: required, max 255 characters
  - `password`: required, max 1024 characters

**File:** [`login-form.tsx`](frontend/src/components/forms/login-form.tsx:68)

#### Step 2: Form Submission
- User clicks "Sign in" button
- `form.handleSubmit(onSubmit)` validates all fields
- If validation fails, field-level error messages appear below inputs
- If validation passes, `onSubmit` is called with validated values

**File:** [`login-form.tsx`](frontend/src/components/forms/login-form.tsx:78)

#### Step 3: Auth Provider Login
- `onSubmit` calls `login(credentials, { rememberMe })` from `useAuth()` hook
- `login` is a `useCallback` in `AuthProvider` that:
  1. Calls `loginMutation(credentials).unwrap()` (RTK Query)
  2. On success: maps `TokenResponse` to `AuthUser`, updates Redux store, sets cookies, shows success toast
  3. On failure: extracts error message, shows error toast, re-throws error

**File:** [`auth-provider.tsx`](frontend/src/components/providers/auth-provider.tsx:176)

#### Step 4: Axios Request
- RTK Query's `axiosBaseQuery` creates an Axios request:
  - URL: `/auth/login` (combined with base URL `http://localhost:8080/api/v1`)
  - Method: `POST`
  - Body: `{ tenantId, username, password }`
- Request interceptor adds:
  - `Authorization: Bearer {accessToken}` (none for login)
  - `X-Tenant-ID: {tenantId}` (from token store, if available)

**File:** [`api.ts`](frontend/src/services/api.ts:40), [`interceptors.ts`](frontend/src/services/interceptors.ts:36)

#### Step 5: Backend Processing
- Backend `AuthController.login()` receives the request
- `AuthServiceImpl.login()`:
  1. Finds user by `tenantId` + `username` (or email)
  2. Checks if user is `ACTIVE`
  3. Checks if account is temporarily locked
  4. Verifies password hash
  5. On success: generates JWT access token + opaque refresh token, stores refresh token hash
  6. Returns `TokenResponse` with tokens and user info

**File:** [`AuthServiceImpl.java`](platform/identity/src/main/java/com/erp/platform/identity/application/AuthServiceImpl.java:71)

#### Step 6: Backend Response
- Success (200 OK): Returns `TokenResponse` with `accessToken`, `refreshToken`, `userId`, `tenantId`, `roles`, etc.
- Failure (401): Returns `ApiError` with `code: "INVALID_CREDENTIALS"` and message
- Failure (403): Returns `ApiError` with `code: "ACCOUNT_LOCKED"` and message
- Failure (422): Returns `ApiError` with `code: "VALIDATION_ERROR"` and field error

**File:** [`GlobalExceptionHandler.java`](platform/identity/src/main/java/com/erp/platform/identity/interfaces/rest/GlobalExceptionHandler.java:84)

#### Step 7: Store Update
- On success, `login()` in `AuthProvider`:
  1. Maps response to `AuthUser` shape
  2. Dispatches `setCredentials()` to Redux store
  3. Updates in-memory token store (`setTokenSnapshot`)
  4. Sets HTTP cookies (`setAuthCookies`) with appropriate max-age
  5. Returns `AuthUser` to caller

**File:** [`auth-provider.tsx`](frontend/src/components/providers/auth-provider.tsx:193), [`auth.slice.ts`](frontend/src/store/slices/auth.slice.ts:107)

#### Step 8: Toast Notification
- Success: `toast.success("Welcome back, {fullName}!")` appears in top-right corner
- Error: `toast.error("Sign in failed", { description: message })` appears
- Account locked: `toast.error("Account locked", { description: message, duration: 6000 })` with longer display

**File:** [`auth-provider.tsx`](frontend/src/components/providers/auth-provider.tsx:210)

#### Step 9: Redirect
- On success, `router.push("/")` navigates to the application root
- `router.refresh()` ensures server components re-render with new auth state
- `ProtectedRoute` component verifies authentication before rendering protected content

**File:** [`login-form.tsx`](frontend/src/components/forms/login-form.tsx:86), [`protected-route.tsx`](frontend/src/components/auth/protected-route.tsx:34)

### Error Handling Flow

```
┌─────────────┐
│   Error     │
│  occurs     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Error Source                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Network   │  │   HTTP      │  │  Backend             │  │
│  │   Error     │  │   4xx/5xx   │  │  Validation Error    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│              getApiErrorMessage(error)                       │
│                                                               │
│  1. Check for backend error code (e.g., "ACCOUNT_LOCKED")   │
│  2. Map to user-friendly message                             │
│  3. Fall back to HTTP status code message                    │
│  4. Fall back to raw error message                           │
│  5. Final fallback: "An unexpected error occurred"           │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Error Display                             │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Standard Error (red)                                   │ │
│  │  "Invalid username or password. Please check your       │ │
│  │   credentials and try again."                           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Account Locked (amber warning)                         │ │
│  │  ⚠ Your account has been temporarily locked...         │ │
│  │  If you need immediate access, contact your             │ │
│  │  administrator or use the password reset option.        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Toast Notification                        │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Success    │  │  Error      │  │  Warning (locked)   │  │
│  │  (green)    │  │  (red)      │  │  (amber, 6s)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Session Management

### Token Storage Strategy

| Token | Storage Location | Purpose | Lifetime |
|-------|-----------------|---------|----------|
| `accessToken` | In-memory (token store) + Cookie | API authentication | 15 minutes |
| `refreshToken` | Cookie only | Obtain new access tokens | 7 days (30 days if "Remember me") |
| `tenantId` | Cookie + In-memory | Multi-tenant context | Matches refresh token |

### Token Refresh Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  API Request│────▶│  401        │────▶│  Interceptor│
│  with       │     │  Response   │     │  catches    │
│  expired    │     │             │     │  error      │
│  token      │     └─────────────┘     └──────┬──────┘
└─────────────┘                                │
                                                ▼
                                        ┌─────────────┐
                                        │  Call       │
                                        │  refresh()  │
                                        │  handler    │
                                        └──────┬──────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │  POST /auth/refresh │
                                    │  with refreshToken  │
                                    └──────────┬──────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │  New token pair     │
                                    │  returned           │
                                    └──────────┬──────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │  Update token store │
                                    │  + cookies          │
                                    │  + retry original   │
                                    └──────────┬──────────┘
                                               │
                                    ┌──────────▼──────────┐
                                    │  Original request   │
                                    │  retried with new   │
                                    │  access token       │
                                    └─────────────────────┘
```

### Session Restoration on App Load

When the user refreshes the page or opens the app in a new tab:

1. `AuthProvider` mounts and runs `restoreSession()`
2. Reads `accessToken`, `refreshToken`, `tenantId` from cookies
3. If tokens exist, sets them in the token store and Redux state
4. Calls `GET /auth/me` to validate the access token
5. If `/auth/me` succeeds: user is authenticated, loads profile
6. If `/auth/me` fails (401): attempts `POST /auth/refresh`
7. If refresh succeeds: updates tokens, retries `/auth/me`
8. If refresh fails: clears all auth state, redirects to `/login`

**File:** [`auth-provider.tsx`](frontend/src/components/providers/auth-provider.tsx:116)

---

## File Reference

| File | Purpose |
|------|---------|
| [`login-form.tsx`](frontend/src/components/forms/login-form.tsx) | Login form component with validation and submission |
| [`auth-provider.tsx`](frontend/src/components/providers/auth-provider.tsx) | Auth context provider with login/logout/refresh logic |
| [`auth.slice.ts`](frontend/src/store/slices/auth.slice.ts) | Redux slice for auth state management |
| [`auth.service.ts`](frontend/src/services/auth.service.ts) | RTK Query API definitions for auth endpoints |
| [`api.ts`](frontend/src/services/api.ts) | RTK Query base query with Axios integration |
| [`http-client.ts`](frontend/src/services/http-client.ts) | Shared Axios instance configuration |
| [`interceptors.ts`](frontend/src/services/interceptors.ts) | Axios request/response interceptors for auth |
| [`token-store.ts`](frontend/src/services/token-store.ts) | In-memory token registry for interceptors |
| [`error-handler.ts`](frontend/src/services/error-handler.ts) | Centralized error message extraction and mapping |
| [`auth.ts`](frontend/src/lib/auth.ts) | Cookie-based token storage utilities |
| [`env.ts`](frontend/src/config/env.ts) | Environment variable validation |
| [`forgot-password/page.tsx`](frontend/src/app/forgot-password/page.tsx) | Password reset request page |
| [`protected-route.tsx`](frontend/src/components/auth/protected-route.tsx) | Route guard for authenticated pages |

---

## Environment Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NEXT_PUBLIC_APP_URL` | `http://localhost:3000` | Frontend application URL |
| `API_BASE_URL` | `http://localhost:8080/api/v1` | Backend API base URL |
| `API_TIMEOUT_MS` | `30000` | Request timeout in milliseconds |
| `NEXT_PUBLIC_ENABLE_DEBUG_LOGGING` | `false` | Enable debug logging |

**File:** [`env.ts`](frontend/src/config/env.ts:12)

---

## Security Considerations

1. **Tokens are never logged**: Access and refresh tokens are only stored in memory and cookies
2. **Short-lived access tokens**: 15-minute expiration limits the window for token theft
3. **Refresh token rotation**: Each refresh issues a new refresh token and revokes the old one
4. **Account lockout**: After 5 failed attempts, account is locked for a configurable duration
5. **HTTPS in production**: Cookies use `Secure` flag in production
6. **SameSite cookies**: Prevents CSRF attacks
7. **Multi-tenant isolation**: `X-Tenant-ID` header ensures data isolation per tenant

---

## Backend API Contract

### Login Request
```json
POST /api/v1/identity/auth/login
{
  "tenantId": 1,
  "username": "user@example.com",
  "password": "raw-password",
  "deviceInfo": "optional-device-info",
  "ipAddress": "optional-ip-address"
}
```

### Login Response (Success - 200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "opaque-refresh-token-value",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 604800,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "roles": ["ADMIN", "USER"]
}
```

### Error Response (Failure)
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid username or password",
  "path": "/api/v1/identity/auth/login",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Network Error" toast | Backend not running or CORS issue | Ensure backend is running on `localhost:8080` |
| "Session expired" | Access token expired, refresh failed | Clear cookies and login again |
| "Account locked" | Too many failed login attempts | Wait for lockout period or use password reset |
| "Invalid credentials" | Wrong username/password | Verify credentials, check tenant ID |
| Stale data after login | Cache not invalidated | RTK Query automatically invalidates `Auth` tag on login |

### Debug Mode

Enable debug logging by setting `NEXT_PUBLIC_ENABLE_DEBUG_LOGGING=true` in `.env.local`.
