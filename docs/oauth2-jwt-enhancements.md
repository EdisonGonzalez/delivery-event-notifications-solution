# OAuth 2.0 with JWT Enhancements Breakdown

This document provides a detailed breakdown for implementing OAuth 2.0 compliant JWT authentication with the requested enhancements.

## Overview

The current implementation is a custom JWT solution. To move toward OAuth 2.0 compliance while maintaining the benefits of JWT, we need to implement the following enhancements.

## 1. Handle ROLES with Access to Endpoints

### Current State
- All users have `ROLE_CLIENT` authority
- No role-based access control on endpoints

### Implementation Plan

**Step 1: Define Roles**
```java
// Create Role enum or constants
public enum Role {
    CLIENT,
    ADMIN,
    SYSTEM,
    READ_ONLY
}
```

**Step 2: Update User Entity**
```sql
-- Add roles table
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    UNIQUE(user_id, role)
);

-- Migration V5__create_user_roles.sql
```

**Step 3: Update CustomUserDetails**
```java
public class CustomUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final String clientId;
    private final List<GrantedAuthority> authorities; // Load from database
    
    // Constructor now accepts roles from database
    public CustomUserDetails(String username, String password, String clientId, List<String> roles) {
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}
```

**Step 4: Update CustomUserDetailsService**
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserEntity user = userRepository.findByUsernameAndEnabled(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    
    List<String> roles = userRoleRepository.findRolesByUserId(user.getId());
    
    return new CustomUserDetails(
        user.getUsername(), 
        user.getPassword(), 
        user.getClientId(),
        roles
    );
}
```

**Step 5: Add Role-Based Access Control**
```java
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    // Admin-only endpoints
}

@RestController
@RequestMapping("/notification_events")
public class NotificationEventController {
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<List<NotificationEvent>> listEvents() {
        // All authenticated users can list
    }
    
    @PostMapping("/{id}/replay")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Void> replayEvent(@PathVariable UUID id) {
        // CLIENT and ADMIN can replay
    }
}
```

**Step 6: Update SecurityConfig**
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // Enable method-level security with @PreAuthorize
}
```

### Files to Modify
- `src/main/resources/db/migration/V5__create_user_roles.sql` (new)
- `src/main/java/.../persistence/jpa/UserRoleEntity.java` (new)
- `src/main/java/.../persistence/jpa/UserRoleJpaRepository.java` (new)
- `src/main/java/.../config/CustomUserDetails.java`
- `src/main/java/.../config/CustomUserDetailsService.java`
- `src/main/java/.../config/SecurityConfig.java`
- `src/main/java/.../web/NotificationEventController.java`

---

## 2. Implement Token Refresh Mechanism

### Current State
- Single JWT token with fixed expiration
- No refresh token mechanism

### Implementation Plan

**Step 1: Add Refresh Token Entity**
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    revoked BOOLEAN NOT NULL DEFAULT false
);

-- Migration V6__create_refresh_tokens.sql
```

**Step 2: Update JwtUtil for Refresh Tokens**
```java
public class JwtUtil {
    private long jwtRefreshExpirationMs;
    
    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }
    
    public boolean isRefreshToken(String token) {
        // Refresh tokens don't have clientId claim
        return extractClientId(token) == null;
    }
}
```

**Step 3: Create RefreshTokenService**
```java
@Service
public class RefreshTokenService {
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    
    public RefreshToken createRefreshToken(String username) {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtUtil.generateRefreshToken(username));
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtRefreshExpirationMs));
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired");
        }
        return token;
    }
    
    public String refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
            .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token not found"));
        
        verifyExpiration(refreshToken);
        
        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException(requestRefreshToken, "Refresh token is revoked");
        }
        
        UserEntity user = refreshToken.getUser();
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getClientId());
        
        return accessToken;
    }
}
```

**Step 4: Add Refresh Endpoint**
```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final RefreshTokenService refreshTokenService;
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
        @RequestBody TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();
        String accessToken = refreshTokenService.refreshToken(refreshToken);
        return ResponseEntity.ok(new TokenRefreshResponse(accessToken, refreshToken));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        refreshTokenService.deleteByToken(request.refreshToken());
        return ResponseEntity.ok().build();
    }
    
    record TokenRefreshRequest(String refreshToken) {}
    record TokenRefreshResponse(String accessToken, String refreshToken) {}
    record LogoutRequest(String refreshToken) {}
}
```

**Step 5: Update Configuration**
```yaml
# application.yml
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long-for-hs256
  expiration: 86400000  # 24 hours for access token
  refresh-expiration: 604800000  # 7 days for refresh token
```

### Files to Modify
- `src/main/resources/db/migration/V6__create_refresh_tokens.sql` (new)
- `src/main/java/.../persistence/jpa/RefreshTokenEntity.java` (new)
- `src/main/java/.../persistence/jpa/RefreshTokenJpaRepository.java` (new)
- `src/main/java/.../config/JwtUtil.java`
- `src/main/java/.../service/RefreshTokenService.java` (new)
- `src/main/java/.../web/AuthController.java`
- `src/main/resources/application.yml`

---

## 3. Token Expiration Validation in Filter

### Current State
- Token expiration is validated by JwtUtil.validateToken()
- No explicit expiration check in filter

### Implementation Plan

**Step 1: Update JwtUtil**
```java
public class JwtUtil {
    public boolean isTokenExpired(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.before(new Date());
    }
    
    public long getExpirationTime(String token) {
        return getClaims(token).getExpiration().getTime();
    }
}
```

**Step 2: Update JwtAuthenticationFilter**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Explicit expiration check
            if (jwtUtil.isTokenExpired(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Token expired\"}");
                return;
            }
            
            if (jwtUtil.validateToken(token)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**Step 3: Add Token Expiration Exception Handler**
```java
@RestControllerAdvice
public class TokenExceptionHandler {
    
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, String>> handleTokenExpired(TokenExpiredException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Token expired");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

### Files to Modify
- `src/main/java/.../config/JwtUtil.java`
- `src/main/java/.../config/JwtAuthenticationFilter.java`
- `src/main/java/.../web/exception/TokenExceptionHandler.java` (new)

---

## 4. Rate Limiting on /auth/login Endpoint

### Current State
- No rate limiting on login endpoint
- Vulnerable to brute force attacks

### Implementation Plan

**Step 1: Add Bucket4j Dependency**
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-jcache</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Step 2: Create Rate Limiting Service**
```java
@Service
public class RateLimitingService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    private Bucket createNewBucket() {
        // 5 requests per minute
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build();
    }
    
    public boolean tryConsume(String key) {
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());
        return bucket.tryConsume(1);
    }
}
```

**Step 3: Add Rate Limiting Filter**
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RateLimitingService rateLimitingService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if ("/auth/login".equals(path)) {
            String clientIp = getClientIp(request);
            
            if (!rateLimitingService.tryConsume(clientIp)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\": \"Too many requests\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

**Step 4: Update SecurityConfig**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            // ... rest of configuration
    }
}
```

### Files to Modify
- `pom.xml`
- `src/main/java/.../service/RateLimitingService.java` (new)
- `src/main/java/.../config/RateLimitingFilter.java` (new)
- `src/main/java/.../config/SecurityConfig.java`

---

## 5. Audience and Issuer Claims

### Current State
- No audience or issuer claims in JWT tokens
- No validation of these claims

### Implementation Plan

**Step 1: Update Configuration**
```yaml
# application.yml
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long-for-hs256
  expiration: 86400000
  refresh-expiration: 604800000
  audience: delivery-event-notifications-api
  issuer: delivery-event-notifications-service
```

**Step 2: Update JwtUtil**
```java
@Component
public class JwtUtil {
    @Value("${jwt.audience}")
    private String jwtAudience;
    
    @Value("${jwt.issuer}")
    private String jwtIssuer;
    
    public String generateToken(String username, String clientId) {
        return Jwts.builder()
            .subject(username)
            .claim("clientId", clientId)
            .audience().add(jwtAudience).and()
            .issuer(jwtIssuer)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            
            Claims claims = claimsJws.getPayload();
            
            // Validate audience
            if (!jwtAudience.equals(claims.getAudience())) {
                return false;
            }
            
            // Validate issuer
            if (!jwtIssuer.equals(claims.getIssuer())) {
                return false;
            }
            
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

**Step 3: Update JwtAuthenticationFilter**
```java
@Override
protected void doFilterInternal(
    HttpServletRequest request, 
    HttpServletResponse response, 
    FilterChain filterChain) throws ServletException, IOException {
    
    String authHeader = request.getHeader("Authorization");
    
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    String token = authHeader.substring(7);
    String username = jwtUtil.extractUsername(token);
    
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        if (jwtUtil.validateToken(token)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    
    filterChain.doFilter(request, response);
}
```

### Files to Modify
- `src/main/resources/application.yml`
- `src/main/java/.../config/JwtUtil.java`

---

## Token Revocation/Blacklisting for PoC

### Recommendation: NOT Required for PoC

**Rationale:**
1. **Complexity**: Token revocation requires maintaining a blacklist (Redis, database) which adds significant complexity
2. **Short-lived Tokens**: With 24-hour access tokens and 7-day refresh tokens, the risk window is manageable for a PoC
3. **Alternative**: Users can change passwords to invalidate all tokens
4. **Simplicity**: For a PoC, the refresh token mechanism provides sufficient security

**If Needed in Future:**
```java
@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token) {
        long expiration = jwtUtil.getExpirationTime(token);
        long ttl = expiration - System.currentTimeMillis();
        redisTemplate.opsForValue().set("blacklist:" + token, "true", ttl, TimeUnit.MILLISECONDS);
    }
    
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}
```

---

## Implementation Priority

### High Priority (Security Critical)
1. **Token Expiration Validation** - Already partially implemented, needs explicit filter check
2. **Rate Limiting** - Critical for preventing brute force attacks
3. **Audience and Issuer Claims** - Important for token validation

### Medium Priority (Functionality)
4. **ROLES with Access Control** - Important for multi-tenant scenarios
5. **Token Refresh Mechanism** - Improves UX and security

### Low Priority (PoC)
6. **Token Revocation/Blacklisting** - Not needed for PoC

---

## Testing Strategy

For each enhancement:
1. Unit tests for individual components
2. Integration tests for API endpoints
3. Security tests for edge cases
4. Load tests for rate limiting

---

## Migration Strategy

1. **Phase 1**: Add new database migrations
2. **Phase 2**: Implement core changes (JWT util, filter)
3. **Phase 3**: Add new endpoints and services
4. **Phase 4**: Update existing controllers with role-based access
5. **Phase 5**: Update Postman collection and documentation
6. **Phase 6**: Deploy and monitor
