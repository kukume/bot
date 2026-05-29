# Logic Module

## Structure

- `entity/` — Exposed `IntEntity` + `IntIdTable`，继承 `BaseIntEntity` / `BaseIntIdTable`
- `logic/` — 平台业务逻辑，纯 suspend 函数，无 UI / 框架依赖
- `job/` — 定时任务，使用 `CoroutineJob` 调度
- `utils/` — 通用工具函数

## Entity Patterns

```kotlin
object BaiduTable : BaseIntIdTable("baidu") {
    val cookie = text("cookie")
    val sign = enumeration<Status>("sign").default(Status.OFF)
}

class BaiduEntity(id: EntityID<Int>) : BaseIntEntity(id, BaiduTable) {
    companion object : BaseIntEntityClass<BaiduEntity>(BaiduTable)
    var cookie by BaiduTable.cookie
    var sign by BaiduTable.sign
}
```

- 所有实体继承 `BaseIntEntity` / `BaseIntIdTable`（自带 `identityId`, `identityName`, `created`, `modified`）
- `Status` 枚举表示开关状态（ON / OFF）
- 多账号通过 `identityName` 区分，同一用户通过 `identityId` 区分

## Database Access

- 所有 DB 操作必须在 `suspendTransaction { }` 中执行
- 优先使用 `upsert` 处理绑定/更新逻辑
- 查询条件组合用 `and` / `andWhere`

## Logic Patterns

- 命名：`{Platform}Logic`，函数为 suspend
- 登录流程：获取凭证 → 验证 → 返回实体（不直接操作 DB）
- 签到流程：接收实体 → 执行 HTTP 请求 → 返回结果字符串或抛异常

## Anti-Patterns

- Don't perform DB writes in Logic classes — return data to caller
- Don't use blocking HTTP calls — Ktor client is suspend-only
- Don't hardcode user-facing strings in Logic — keep in Handler
- Don't expose raw exceptions to users — wrap in domain messages
