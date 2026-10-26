# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `SECURITY.md` security policy
- `CODE_OF_CONDUCT.md` contributor covenant
- Maven wrapper for reproducible builds
- JitPack publishing instructions and dependency coordinates
- `AuthInterceptorTest` coverage for missing, invalid, expired, valid, and excluded-token requests

### Changed

- 401 responses now distinguish `TOKEN_MISSING`, `TOKEN_INVALID`, and `TOKEN_EXPIRED`
- README now documents explicit `jwt.exclude-paths` instead of claiming built-in default whitelist paths
- JJWT version references aligned with 0.13.x
- README adds a 30-second overview and shortest demo; release tags are documented as `v1.1.0`

## [1.0.0] - 2026-08-12

### Added

- JWT auto configuration: `JwtAutoConfiguration`, `JwtUtil` (JJWT 0.13.x, HMAC-SHA), `AuthInterceptor`, `TokenExpiredData`
- Configurable exclude paths with Ant-style patterns
- Path traversal protection for `../` and `..\\`
- `TOKEN_EXPIRED` marker for expired tokens
- Unit tests for `JwtUtil`
