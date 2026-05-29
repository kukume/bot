# Telegram Module

## Framework

- `io.github.dehuckakpyt.telegrambot` — declarative bot DSL
- Entry: `TelegramApplication.kt`

## Handler Patterns

- 类继承 `BotHandler({ ... })`，标注 `@Factory`
- 命令用 `command("/xxx") { }`
- 回调用 `callback("key") { }`，数据通过 `transferred<T>()` 反序列化
- 多步交互用 `step("key") { }` + `next("nextKey", data)`
- 使用 `suspendTransaction { }` 进行数据库操作

## Job Patterns

- 定时任务在 `job/` 下，继承调度框架约定
- 调用对应 `Logic` 的 suspend 函数执行签到
- 任务内处理异常，避免一个平台失败阻断其他平台

## Data Flow

1. 用户发送 `/sign` → `LoginHandler.command("/sign")`
2. 选择平台 → `callback("selectIdentity")` → 选择身份
3. 登录/管理/执行 → 调用 `Logic` → `suspendTransaction` 读写 DB
4. 返回结果通过 `sendMessage` / `editMessageText` 展示

## Anti-Patterns

- Don't call DB outside `suspendTransaction`
- Don't store mutable state in Handler classes — use DB or `transferred` payload
- Don't block the event loop with `delay` without timeout guards
- Don't leak raw exception messages to chat — sanitize user-facing text
