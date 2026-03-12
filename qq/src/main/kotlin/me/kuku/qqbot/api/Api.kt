package me.kuku.qqbot.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*

open class BaseRequest<T: BaseResponse> (
    @JsonIgnore
    val requestUrl: String,
    @JsonIgnore
    val method: HttpMethod = HttpMethod.Post
)

open class BaseResponse

data class Ark(
    @field:JsonProperty("template_id")
    val templateId: Int,
    val kv: List<Kv>
) {

    data class Kv(
        val key: String,
        val value: String? = null,
        val obj: Obj? = null
    )

    data class Obj(
        @field:JsonProperty("obj_kv")
        val objKv: List<ObjKv>
    )

    data class ObjKv(
        val key: String,
        val value: String
    )

}

data class Markdown(
    val content: String? = null,
    @field:JsonProperty("custom_template_id")
    val customTemplateId: String? = null,
    val params: List<Ark.Kv>? = null
)

data class Media(
    @field:JsonProperty("file_info")
    val fileInfo: String
)