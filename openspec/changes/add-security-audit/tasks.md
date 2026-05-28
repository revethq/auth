## 1. OAuth 2.1 / OIDC Protocol Compliance Audit
- [ ] 1.1 Audit PKCE enforcement — verify `code_challenge` is required, `plain` method is rejected, `code_verifier` is validated on token exchange, PKCE downgrade attack prevention (ASVS 10.4.6, RFC 9700 §4.8)
- [ ] 1.2 Audit redirect URI validation — test exact string matching, normalization bypasses, path traversal, HTTP parameter pollution, open redirect chaining (ASVS 10.4.1, RFC 9700 §4.1, §4.11)
- [ ] 1.3 Audit authorization code security — verify single-use with revocation on reuse, expiration (max 10min), client binding, code injection prevention (ASVS 10.4.2, 10.4.3, RFC 9700 §4.5)
- [ ] 1.4 Audit state parameter handling — verify passthrough and CSRF protection (RFC 9700 §4.7)
- [ ] 1.5 Audit response type and grant type restriction — flag `token` (implicit) and ROPC as non-compliant, assess hybrid flow, verify response mode restriction per client (ASVS 10.4.4, 10.6.1, RFC 9700 §2.1.2, §2.4)
- [ ] 1.6 Audit mix-up attack surface — verify `iss` in responses, cross-server code injection prevention (ASVS 10.2.2, RFC 9700 §4.4)
- [ ] 1.7 Audit OIDC discovery endpoint — verify metadata accuracy and no sensitive information leakage
- [ ] 1.8 Audit clickjacking protection — verify X-Frame-Options and CSP frame-ancestors on authorization/login pages (RFC 9700 §4.16)
- [ ] 1.9 Audit credential leakage via browser — verify Referrer-Policy headers, 302 (not 307) redirects after credential submission, browser history exposure (RFC 9700 §4.2, §4.3, §4.12)
- [ ] 1.10 Audit scope upgrade attacks — verify scopes cannot be escalated during code exchange or refresh (RFC 9700 §2.3)
- [ ] 1.11 Audit consent management — document user consent prompting, scope disclosure, and consent review/revocation capabilities (ASVS 10.7.1, 10.7.2, 10.7.3)
- [ ] 1.12 Audit host header poisoning — verify redirect URIs and issuer claims are not constructed from the Host header
- [ ] 1.13 Document findings for OAuth/OIDC compliance area

## 2. Token Security Audit
- [ ] 2.1 Audit JWT signing — review RS256 implementation, key strength, `kid` header, algorithm confusion prevention
- [ ] 2.2 Audit refresh token security — verify entropy, hash storage (SHA-256 assessment), rotation, reuse detection with family revocation, scope binding, absolute expiration, revocation capability (ASVS 10.4.5, 10.4.8, 10.4.9, RFC 9700 §4.14)
- [ ] 2.3 Audit token expiration — verify enforcement for access tokens, refresh tokens (absolute lifetime), and authorization codes (max 10min L1/L2, 1min L3 per ASVS 10.4.3)
- [ ] 2.4 Audit JWT claim validation — verify `iss`, `aud`, `sub` claim generation and validation, delegated authorization claims (ASVS 10.3.1, 10.3.2, 10.3.3)
- [ ] 2.5 Audit token leakage — search for tokens in logs, error responses, URLs; verify token distribution minimization (ASVS 10.1.1, RFC 9700 §4.2, §4.3, §4.9)
- [ ] 2.6 Audit sender-constrained tokens — assess DPoP/mTLS support feasibility, document stolen token replay risk (ASVS 10.3.5, 10.4.14, RFC 9700 §4.10)
- [ ] 2.7 Audit confidential client backchannel authentication — verify client auth on token endpoint, assess authentication method strength (ASVS 10.4.10, 10.4.16)
- [ ] 2.8 Document findings for token security area

## 3. Credential & Secret Management Audit
- [ ] 3.1 Audit password hashing — review BCrypt work factor, hash storage, timing-safe comparison (`UserService.kt`)
- [ ] 3.2 Audit client secret security — review generation entropy, hash storage, comparison method (`ApplicationService.kt`, `ApplicationSecret.kt`)
- [ ] 3.3 Audit default credentials — search all `application.properties`, env var defaults, and hardcoded values
- [ ] 3.4 Audit signing key generation — review RSA key generation, `SecureRandom` usage, private key storage (`AuthorizationServerService.kt`, `SigningKey.kt`)
- [ ] 3.5 Document findings for credential management area

## 4. OWASP Web API Security Audit
- [ ] 4.1 Audit injection vectors — review all Panache queries for HQL injection, Qute templates for template injection, JSON-B deserialization (`persistence/repositories/`, `web/authorization/`)
- [ ] 4.2 Audit access control — review authentication requirements on management API endpoints, resource ownership validation, horizontal privilege escalation (OWASP API2:2023, API5:2023)
- [ ] 4.3 Audit security misconfiguration — review CORS (all-origin policy), TLS trust-all, TLS terminating proxy exposure, Swagger UI, dev settings, security headers (Strict-Transport-Security, X-Content-Type-Options, Cache-Control) (RFC 9700 §4.13)
- [ ] 4.4 Audit rate limiting — assess brute-force protection on login, token, and API endpoints
- [ ] 4.5 Audit error handling — review exception mappers for stack trace exposure, user enumeration via auth errors, OAuth error compliance (RFC 6749 §5.2)
- [ ] 4.6 Audit SSRF — review SCIM outbound URL handling, redirect URI server-side behavior
- [ ] 4.7 Document findings for OWASP web API security area

## 5. Multi-Tenancy Isolation Audit
- [ ] 5.1 Audit cross-tenant data access — verify authorization server scoping on user, client, application, and code queries (`persistence/services/`, `persistence/repositories/`)
- [ ] 5.2 Audit signing key isolation — verify JWKS endpoint scoping and token signing key selection
- [ ] 5.3 Audit scope boundary enforcement — verify scope definitions and token scope grants are tenant-scoped
- [ ] 5.4 Audit tenant identifier validation — verify `authorizationServerId` path parameter validation and context propagation
- [ ] 5.5 Document findings for multi-tenancy isolation area

## 6. Quarkus & JVM Best Practices Audit
- [ ] 6.1 Audit dependency vulnerabilities — run CVE check on all direct and transitive dependencies, flag outdated security libraries
- [ ] 6.2 Audit Hibernate ORM patterns — review entity exposure, lazy loading DoS risk, schema management strategy
- [ ] 6.3 Audit CDI scope correctness — review singleton services for request-state leakage, verify security context scoping
- [ ] 6.4 Audit native build configuration — review reflection config, GraalVM security features
- [ ] 6.5 Audit configuration secrets management — review env var security, production profile separation
- [ ] 6.6 Document findings for Quarkus/JVM practices area

## 7. SCIM Provisioning Security Audit
- [ ] 7.1 Audit SCIM outbound authentication — review JWT generation for SCIM, bearer token transmission, token lifetime (`ScimClient.kt`, SCIM processors)
- [ ] 7.2 Audit SCIM URL validation — review base URL validation, URL construction safety, DNS rebinding risk
- [ ] 7.3 Audit SCIM delivery security — review payload sensitivity, retry safety, event processor isolation
- [ ] 7.4 Document findings for SCIM provisioning security area

## 8. Logging & Audit Trail Audit
- [ ] 8.1 Audit sensitive data in logs — search for token, credential, and key material logging across entire codebase
- [ ] 8.2 Audit authentication event tracking — review login attempt, token grant, admin action, and revocation tracking completeness
- [ ] 8.3 Audit event system integrity — review Event entity immutability, EventType completeness, storage reliability
- [ ] 8.4 Document findings for logging and audit trail area
