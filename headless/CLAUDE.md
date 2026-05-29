# Headless Module

## Purpose

Headless browser service using Playwright. Exposes HTTP endpoints for other modules to execute browser automation.

## Structure

- `controller/` — Ktor HTTP controllers (`ExecController`, `IndexController`)
- `ktor/` — Server setup
- `utils/` — Playwright utilities

## Patterns

- Controllers receive requests, delegate to `PlaywrightUtils`
- Each request should create and close browser context to avoid state leak
- Return structured JSON responses

## Anti-Patterns

- Don't share browser contexts across requests — isolate per call
- Don't leave pages open — always close in `finally`
- Don't run Playwright on the main thread without coroutine dispatcher
- Don't return raw HTML / screenshots without explicit caller consent
