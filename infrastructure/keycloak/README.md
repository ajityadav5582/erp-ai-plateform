# Keycloak Infrastructure

Identity and Access Management (IAM) for the ERP AI Platform using Keycloak.

## Directory Structure

```
infrastructure/keycloak/
├── config/
│   ├── realm.json              # Realm configuration
│   ├── keycloak-postgres.env     # PostgreSQL integration config
│   └── keycloak-dev.env          # Development configuration
├── themes/                     # Custom themes (optional)
├── backups/                    # Backup scripts and docs
│   └── README.md
└── README.md                   # This file
```

## Quick Start

### Start Keycloak

```bash
# Start with development configuration (in-memory DB)
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up keycloak

# Access Keycloak Admin Console
open http://localhost:8080/
```

### PostgreSQL Integration

For production, use PostgreSQL for persistent storage:

```bash
# Create keycloak database and user in PostgreSQL
docker compose exec postgres psql -U erpai -d erpai_platform -c "
CREATE USER keycloak WITH PASSWORD 'keycloak_dev_password';
GRANT ALL PRIVILEGES ON DATABASE erpai_platform TO keycloak;
"

# Start with PostgreSQL configuration
KEYCLOAK_DB_PASSWORD=keycloak_dev_password \
docker compose -f compose.base.yml -f compose.infrastructure.yml up keycloak
```

## Configuration

### Realm Configuration

The realm configuration is in [`config/realm.json`](config/realm.json). It includes:

- **Realm:** `erpai`
- **Password Policy:** 12 chars, digits, lowercase, uppercase, special chars
- **Brute Force Protection:** Enabled
- **Token Lifespan:** 15 minutes (configurable)

### Client Placeholders

The following clients are configured as placeholders (no business users):

| Client ID | Type | Purpose |
|-----------|------|---------|
| `erpai-gateway` | confidential | API Gateway (backend services) |
| `erpai-web` | public | Web application |
| `erpai-mobile` | public | Mobile application |
| `erpai-services` | confidential | Service-to-service communication |

### Environment Variables

| Variable | Description | Default |
|----------|-----------|---------|
| `KEYCLOAK_ADMIN` | Admin username | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Admin password | `admin` |
| `KEYCLOAK_DB_PASSWORD` | Database password | `keycloak_dev_password` |
| `KC_DB` | Database type | `dev-mem` (dev), `postgres` (prod) |
| `KC_HTTP_RELATIVE_PATH` | Base path | `/` |

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Keycloak | quay.io/keycloak/keycloak:26.2 | 8080 | Identity provider |

## Health Checks

```bash
# Check Keycloak health
curl -f http://localhost:8080/health/ready
curl -f http://localhost:8080/health/live
```

## Backup and Recovery

See [`backups/README.md`](backups/README.md) for backup procedures.

## Documentation

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Keycloak Integration](https://spring.io/projects/spring-boot)

## Troubleshooting

### Keycloak not starting

```bash
# Check logs
docker compose logs keycloak

# Check health status
docker compose ps keycloak
```

### Database connection issues

```bash
# Verify PostgreSQL is running
docker compose ps postgres

# Check Keycloak database user
docker compose exec postgres psql -U erpai -d erpai_platform -c "\du"
```

## Use Cases
 
 ### 1. Service-to-Service Authentication
 
 **Description:** Backend services authenticate with Keycloak using client credentials flow to access protected resources.
 
 **Use Case:** The `erpai-services` client uses service accounts to obtain JWT tokens for inter-service communication.
 
 ```java
 // Service-to-service authentication
 @Service
 public class ServiceClient {
     
     private final WebClient webClient;
     
     public ServiceClient(WebClient.Builder builder) {
         this.webClient = builder.build();
     }
     
     public Mono<String> callProtectedService() {
         return webClient
             .mutate()
             .filter((request, next) -> {
                 String token = getServiceAccountToken();
                 ClientRequest filtered = ClientRequest.from(request)
                     .header("Authorization", "Bearer " + token)
                     .build();
                 return next.exchange(filtered);
             })
             .build()
             .get()
             .uri("http://service-endpoint/api/data")
             .retrieve()
             .bodyToMono(String.class);
     }
     
     private String getServiceAccountToken() {
         // Use client credentials to get token
         // POST to http://localhost:8080/realms/erpai/protocol/openid-connect/token
         // grant_type: client_credentials
         // client_id: erpai-services
         // client_secret: <secret>
         return "jwt-token";
     }
 }
 ```
 
 ### 2. Web Application Authentication
 
 **Description:** The web application uses the authorization code flow to authenticate users.
 
 **Use Case:** Users log in through the `erpai-web` public client and receive access tokens.
 
 ```java
 // Spring Security configuration for web client
 @Configuration
 @EnableWebSecurity
 public class SecurityConfig {
     
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
             .authorizeHttpRequests(authz -> authz
                 .requestMatchers("/api/public/**").permitAll()
                 .requestMatchers("/api/admin/**").hasRole("admin")
                 .anyRequest().authenticated()
             )
             .oauth2Login(oauth2 -> oauth2
                 .loginPage("/oauth2/authorization/erpai-web")
             );
         return http.build();
     }
 }
 ```
 
 ### 3. API Gateway Authentication
 
 **Description:** The API Gateway validates tokens and forwards requests to backend services.
 
 **Use Case:** The `erpai-gateway` confidential client validates JWT tokens and extracts user information.
 
 ```java
 // Gateway filter to validate tokens
 @Component
 public class JwtAuthenticationFilter extends OncePerRequestFilter {
     
     @Override
     protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
         
         String token = resolveToken(request);
         if (token != null && validateToken(token)) {
             Authentication auth = getAuthentication(token);
             SecurityContextHolder.getContext().setAuthentication(auth);
         }
         filterChain.doFilter(request, response);
     }
     
     private boolean validateToken(String token) {
         try {
             // Validate token against Keycloak JWKS endpoint
             // http://localhost:8080/realms/erpai/protocol/openid-connect/certs
             Jwts.parserBuilder()
                 .setSigningKey(getSigningKey())
                 .build()
                 .parseClaimsJws(token);
             return true;
         } catch (JwtException e) {
             return false;
         }
     }
 }
 ```
 
 ### 4. Mobile Application Authentication
 
 **Description:** Mobile apps use PKCE (Proof Key for Code Exchange) for secure authentication.
 
 **Use Case:** The `erpai-mobile` public client uses PKCE to prevent authorization code interception.
 
 ```java
 // Mobile authentication with PKCE
 public class MobileAuthService {
     
     private static final String AUTH_URL = "http://localhost:8080/realms/erpai/protocol/openid-connect/auth";
     private static final String TOKEN_URL = "http://localhost:8080/realms/erpai/protocol/openid-connect/token";
     
     public void authenticate() {
         // Generate code verifier and challenge
         String codeVerifier = generateCodeVerifier();
         String codeChallenge = generateCodeChallenge(codeVerifier);
         
         // Build authorization URL with PKCE
         String authUrl = AUTH_URL + "?client_id=erpai-mobile"
             + "&response_type=code"
             + "&scope=openid profile email"
             + "&redirect_uri=exp://localhost:19006/callback"
             + "&code_challenge=" + codeChallenge
             + "&code_challenge_method=S256";
         
         // Open browser for authentication
         openBrowser(authUrl);
     }
     
     public TokenResponse exchangeCodeForToken(String code, String codeVerifier) {
         // Exchange authorization code for tokens
         return httpClient.post(TOKEN_URL)
             .param("grant_type", "authorization_code")
             .param("client_id", "erpai-mobile")
             .param("code", code)
             .param("code_verifier", codeVerifier)
             .param("redirect_uri", "exp://localhost:19006/callback")
             .retrieve()
             .bodyToMono(TokenResponse.class)
             .block();
     }
 }
 ```
 
 ### 5. Token Validation in Resource Server
 
 **Description:** Backend services validate JWT tokens and extract user information.
 
 **Use Case:** Protected endpoints validate the `erpai-gateway` client's access tokens.
 
 ```java
 // Resource server configuration
 @Configuration
 @EnableWebSecurity
 public class ResourceServerConfig {
     
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
             .authorizeHttpRequests(authz -> authz
                 .requestMatchers("/api/invoices/**").hasAuthority("SCOPE_invoice:read")
                 .requestMatchers("/api/payments/**").hasAuthority("SCOPE_payment:write")
                 .anyRequest().authenticated()
             )
             .oauth2ResourceServer(oauth2 -> oauth2
                 .jwt(jwt -> jwt
                     .jwtDecoder(jwtDecoder())
                 )
             );
         return http.build();
     }
     
     @Bean
     public JwtDecoder jwtDecoder() {
         return NimbusJwtDecoder.withJwkSetUri(
             "http://localhost:8080/realms/erpai/protocol/openid-connect/certs"
         ).build();
     }
 }
 
 // Controller with role-based access
 @RestController
 @RequestMapping("/api/invoices")
 public class InvoiceController {
     
     @GetMapping("/{id}")
     @PreAuthorize("hasAuthority('SCOPE_invoice:read')")
     public Invoice getInvoice(@PathVariable String id) {
         return invoiceService.findById(id);
     }
     
     @PostMapping
     @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
     public Invoice createInvoice(@RequestBody CreateInvoiceRequest request) {
         return invoiceService.create(request);
     }
 }
 ```
 
 ### 6. User Registration Flow
 
 **Description:** Users register through the application, not directly in Keycloak.
 
 **Use Case:** The application creates users programmatically after business validation.
 
 ```java
 // User registration service
 @Service
 public class UserService {
     
     private final Keycloak keycloak;
     
     public void registerUser(RegisterUserRequest request) {
         // Validate business rules first
         validateBusinessRules(request);
         
         // Create user in Keycloak
         UserRepresentation user = new UserRepresentation();
         user.setUsername(request.email());
         user.setEmail(request.email());
         user.setEnabled(true);
         user.setCredentials(List.of(
             new CredentialRepresentation(
                 request.password(), 
                 "password", 
                 true
             )
         ));
         
         keycloak.realm("erpai").users().create(user);
     }
     
     private void validateBusinessRules(RegisterUserRequest request) {
         // Check if email is unique
         // Check if tenant exists
         // Check if user has permission to create account
     }
 }
 ```
 
 ## Best Practices
 
 - [ ] Use PostgreSQL for production
 - [ ] Enable HTTPS in production
 - [ ] Configure proper password policies
 - [ ] Enable brute force protection
 - [ ] Set appropriate token lifespans
 - [ ] Use custom themes for branding
 - [ ] Regular backup of Keycloak data
 - [ ] Monitor authentication events
