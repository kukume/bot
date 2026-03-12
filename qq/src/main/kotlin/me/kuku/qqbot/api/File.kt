package me.kuku.qqbot.api

import com.fasterxml.jackson.annotation.JsonProperty

open class FileResponse: BaseResponse()

data class PrivateFileRequest(
    val openid: String,
    @field:JsonProperty("file_type")
    val fileType: Int,
    val url: String?,
    @field:JsonProperty("srv_send_msg")
    val srvSendMsg: Boolean = false,
    @field:JsonProperty("file_data")
    val fileData: Any? = null
): BaseRequest<PrivateFileResponse>("/v2/users/$openid/files")

data class PrivateFileResponse(
    @field:JsonProperty("file_uuid")
    val fileUuid: String,
    @field:JsonProperty("file_info")
    val fileInfo: String,
    val ttl: Int,
    val id: String
): FileResponse()



data class GroupFileRequest(
    val openid: String,
    /**
     * 媒体类型：1 图片，2 视频，3 语音，4 文件（暂不开放）
     * 资源格式要求
     * 图片：png/jpg，视频：mp4，语音：silk
     */
    @field:JsonProperty("file_type")
    val fileType: Int,
    val url: String? = null,
    @field:JsonProperty("srv_send_msg")
    val srvSendMsg: Boolean = false,
    @field:JsonProperty("file_data")
    val fileData: Any? = null
): BaseRequest<GroupFileResponse>("/v2/groups/$openid/files")

data class GroupFileResponse(
    @field:JsonProperty("file_uuid")
    val fileUuid: String,
    @field:JsonProperty("file_info")
    val fileInfo: String,
    val ttl: Int,
    val id: String
): FileResponse()

