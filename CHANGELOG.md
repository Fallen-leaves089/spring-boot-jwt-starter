# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.2] - 2026-10-27

### Fixed

- `JwtAutoConfiguration` now builds the interceptor from the container's `JwtUtil` bean, so a user-defined bean is no longer ignored.
- `jwt.exclude-paths` matching strips `server.servlet.context-path`, so whitelist entries describe application-relative paths.

## [1.1.1] - 2026-10-27

### Security

- `jwt.secret` must be at least 32 bytes; missing or short secrets fail fast with a clear message.
- Path traversal checks decode percent-encoded paths up to three times and reject mixed-case and double-encoded dot-dot segments.
- Whitelist wildcards now quote all non-`*` characters, so regex metacharacters are treated literally.

### Fixed

- Whitelist patterns containing regex metacharacters no longer throw or match unintended paths.

## [1.1.0] - 2026-10-26

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
