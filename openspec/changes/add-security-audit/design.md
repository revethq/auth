## Context

Revet Auth is a multi-tenant OAuth 2.1 / OIDC authorization server built on Quarkus + Kotlin. It supports authorization code flow, client credentials flow, refresh tokens, PKCE, SCIM outbound provisioning, and customizable login templates. The codebase has no prior security audit.

The audit needs to be parallelizable across multiple agents, each covering an independent security domain.

## Goals / Non-Goals

**Goals:**
- Identify all security vulnerabilities and misconfigurations across 8 audit areas
- Produce actionable findings with severity, location, and remediation recommendations
- Cover OAuth 2.1 / OIDC-specific threats, OWASP Top 10, platform-specific issues, and multi-tenancy concerns
- Enable parallel execution — each area can be audited independently

**Non-Goals:**
- No code changes in this phase (findings only)
- No penetration testing or runtime exploitation
- No performance testing or load testing
- No third-party dependency source code audit (dependency version checks only)

## Decisions

### Decision: 8 independent audit areas
Each area is a self-contained capability spec with its own tasks. This allows agents to work in parallel without coordination overhead.

**Alternatives considered:**
- Single monolithic audit — rejected because it cannot be parallelized and creates a single massive document
- 3-4 grouped areas — rejected because grouping OAuth+Tokens or OWASP+Config reduces parallelism without meaningful benefit

### Decision: Findings-only first phase
The audit produces documented findings without implementing fixes. A separate follow-up proposal will prioritize and implement remediation.

**Rationale:** Separating discovery from remediation allows the full scope of issues to be understood before committing to fix order. Some findings may be accepted risks, some may be out of scope, and dependencies between fixes may affect implementation order.

### Decision: Structured finding format
All findings use a consistent format (SHORT-ID, severity, location, description, risk, recommendation, references) for machine-parseability and cross-area comparison.

## Risks / Trade-offs

- **False positives** — Static analysis may flag patterns that are safe in context. Mitigated by requiring location-specific analysis, not pattern matching.
- **Incomplete coverage** — An agent may miss vulnerabilities outside its assigned area. Mitigated by deliberate overlap in boundary areas (e.g., OWASP checks token handling, token audit checks OWASP patterns).
- **Stale findings** — Code may change during the audit. Mitigated by referencing specific file:line locations that can be verified.

## Open Questions

- Should the follow-up fix proposal be a single change or split per audit area?
- Are there specific compliance frameworks (SOC2, PCI-DSS) that should influence severity ratings?
