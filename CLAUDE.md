# kukume/bot

多平台自动签到机器人。支持 Telegram / QQ / OneBot，覆盖百度、B站、斗鱼、米哈游、微博等平台的签到与推送任务。

## Development

- Kotlin JVM 21, Gradle, Koin DI, KSP
- ORM: Exposed v1 (suspendTransaction)
- HTTP: Ktor client / server
- Test: JUnit 5 (`./gradlew test`)

## Where to Look

| Task | Location |
|------|----------|
| 添加新平台实体 / 数据库表 | `logic/src/main/kotlin/me/kuku/common/entity/` |
| 添加新平台签到逻辑 | `logic/src/main/kotlin/me/kuku/common/logic/` |
| 添加 Telegram 命令 | `telegram/src/main/kotlin/me/kuku/telegram/handler/` |
| 添加 Telegram 定时任务 | `telegram/src/main/kotlin/me/kuku/telegram/job/` |
| 修改 QQ Bot 事件响应 | `qq/src/main/kotlin/me/kuku/qqbot/event/` |
| 修改 OneBot 命令 | `onebot/src/main/kotlin/me/kuku/onebot/command/` |
| 修改 Headless 浏览器接口 | `headless/src/main/kotlin/me/kuku/headless/controller/` |

## Module Overview

- `logic` — 核心实体（Entity）、数据库表（Table）、公共逻辑（Logic）、定时任务（Job）。被所有其他模块依赖。
- `telegram` — Telegram Bot 入口。Handler 处理交互命令，Job 执行定时签到。
- `qq` — QQ Bot 入口。基于 Ktor 接收事件，通过 API 发送消息。
- `onebot` — OneBot 协议适配。命令映射到逻辑层。
- `headless` — Headless 浏览器服务（Playwright），供其他模块调用。

## Agent Workflow

Explore finds -> Librarian reads -> You plan -> Worker implements -> Validator checks

When delegating to agents:
- Use positive constraints ("ensure X") not negative ("don't do Y")
- Include context, expected output, acceptance criteria
- Launch independent tasks in parallel

## Guidance

Context-specific guidance lives in nested CLAUDE.md files throughout the repo.
These load automatically when you work in those directories.
Closest CLAUDE.md to the file being edited takes precedence.
