# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `SECURITY.md` security policy
- `CODE_OF_CONDUCT.md` contributor covenant
- Maven wrapper for reproducible builds

## [1.0.0] - 2026-08-12

### Added

- JWT auto configuration: `JwtAutoConfiguration`, `JwtUtil` (JJWT 0.12.x, HMAC-SHA), `AuthInterceptor`, `TokenExpiredData`
- Configurable exclude paths with Ant-style patterns
- Path traversal protection for `../` and `..\\`
- `TOKEN_EXPIRED` marker for expired tokens
- Unit tests for `JwtUtil`
