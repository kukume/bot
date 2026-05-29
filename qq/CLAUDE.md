# QQ Module

## Framework

- Ktor server (`EngineMain.main`) — HTTP 入口
- Event-driven: 接收消息事件 → 路由到对应处理逻辑

## Structure

- `api/` — 消息 / 文件 / API 封装
- `context/` — 消息上下文（群聊、私聊）
- `event/` — 事件定义与分发
- `controller/` — Webhook 接收外部推送
- `ktor/` — Ktor 配置与扩展

## Patterns

- 事件处理函数为 suspend
- 通过 `MessageContext` / `GroupMessageContext` 获取消息元数据
- 调用 `logic` 模块的 Logic 类执行业务
- 使用 `api/Message.kt` 中的封装发送回复

## Anti-Patterns

- Don't handle raw JSON in event handlers — use typed events
- Don't bypass `api/` layer to call protocol methods directly
- Don't perform long-running work in event dispatch thread — use coroutines
- Don't mutate context objects after passing them downstream
