/**
 * Security abstractions for the ERP AI Platform.
 *
 * <p>This package provides reusable security abstractions only.
 * It does not implement any specific authentication or authorization
 * mechanisms (like JWT, Keycloak, or OAuth2).
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link com.erp.platform.security.authentication.AuthenticatedUser} - Authenticated user abstraction</li>
 *   <li>{@link com.erp.platform.security.authorization.Permission} - Permission abstraction</li>
 *   <li>{@link com.erp.platform.security.tenant.TenantPrincipal} - Tenant principal</li>
 *   <li>{@link com.erp.platform.security.context.SecurityContext} - Security context abstraction</li>
 *   <li>{@link com.erp.platform.security.authorization.HasPermission} - Authorization annotation</li>
 *   <li>{@link com.erp.platform.security.authorization.HasRole} - Role-based authorization annotation</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.erp.platform.security;
