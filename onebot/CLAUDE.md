# OneBot Module

## Framework

- OneBot protocol adapter
- Entry: `OneBotApplication.kt`

## Structure

- `command/` — 文本命令映射与执行
- `config/` — OneBot 连接配置 (`ROneBot`)

## Patterns

- 命令类处理消息文本匹配，调用 `logic` 模块的 Logic 类
- 配置通过 Koin 注入
- 返回结果通过 OneBot API 发送

## Anti-Patterns

- Don't parse command args manually if a structured parser exists
- Don't embed protocol-specific JSON in command classes — isolate in config
- Don't call blocking I/O — keep all command handlers suspend
- Don't duplicate Logic logic here — delegate to `logic` module
