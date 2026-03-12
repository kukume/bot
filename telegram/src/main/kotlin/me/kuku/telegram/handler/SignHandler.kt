package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.container.CallbackContainer
import io.github.dehuckakpyt.telegrambot.container.Container
import io.github.dehuckakpyt.telegrambot.container.GeneralContainer
import io.github.dehuckakpyt.telegrambot.container.message.TextMessageContainer
import io.github.dehuckakpyt.telegrambot.exception.chat.ChatException
import io.github.dehuckakpyt.telegrambot.ext.container.chatId
import io.github.dehuckakpyt.telegrambot.ext.container.fromId
import io.github.dehuckakpyt.telegrambot.factory.keyboard.button.ButtonFactory
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.handling.BotHandling
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardMarkup
import io.github.dehuckakpyt.telegrambot.model.telegram.Message
import io.github.dehuckakpyt.telegrambot.model.telegram.input.ByteArrayContent
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.kuku.common.entity.*
import me.kuku.common.exception.QrcodeScanException
import me.kuku.common.ktor.client
import me.kuku.common.logic.*
import me.kuku.common.utils.RegexUtils
import me.kuku.common.utils.qrcode
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.koin.core.annotation.Factory
import org.koin.mp.KoinPlatform.getKoin
import java.util.TreeMap

private val loginButtonTreeMap = TreeMap<String, SignData>().also {
    it["baidu"] = SignData("百度", listOf(
        SignData.LoginTypeButton("使用百度系APP扫码登录", "baiduQrcodeLogin")
    ), ManagerData(BaiduEntity, BaiduTable.identityId, BaiduTable.identityName, listOf(
        ManagerData.ManagerButton("签到", BaiduTable.sign)
    ), listOf(
        ManagerData.ExecButton("贴吧签到") { BaiduLogic.tieBaSign(this as BaiduEntity) },
        ManagerData.ExecButton("游帮帮加速器签到") { BaiduLogic.ybbSign(this as BaiduEntity) },
        ManagerData.ExecButton("游帮帮加速器看广告") { BaiduLogic.ybbWatchAd(this as BaiduEntity) },
        ManagerData.ExecButton("游帮帮加速器兑换手游会员") { BaiduLogic.ybbExchangeVip(this as BaiduEntity) }
    )))
    it["biliBili"] = SignData("哔哩哔哩", listOf(
        SignData.LoginTypeButton("使用哔哩哔哩APP扫码登录", "biliBiliQrcodeLogin")
    ), ManagerData(BiliBiliEntity, BiliBiliTable.identityId, BiliBiliTable.identityName, listOf(
        ManagerData.ManagerButton("动态推送", BiliBiliTable.push),
        ManagerData.ManagerButton("签到", BiliBiliTable.sign),
        ManagerData.ManagerButton("开播提醒", BiliBiliTable.live)
    ), listOf(
        ManagerData.ExecButton("哔哩哔哩签到") {
            val firstRank = BiliBiliLogic.ranking(this as BiliBiliEntity)[0]
            BiliBiliLogic.watchVideo(this, firstRank)
            delay(1000)
            BiliBiliLogic.share(this, firstRank.aid)
        },
    )))
    it["douYu"] = SignData("斗鱼", listOf(
        SignData.LoginTypeButton("使用斗鱼APP扫码登录", "douYuQrcodeLogin")
    ), ManagerData(DouYuEntity, DouYuTable.identityId, DouYuTable.identityName, listOf(
        ManagerData.ManagerButton("开播提醒", DouYuTable.live),
        ManagerData.ManagerButton("直播标题变更推送", DouYuTable.titleChange),
        ManagerData.ManagerButton("鱼吧签到", DouYuTable.fishGroup),
        ManagerData.ManagerButton("鱼吧动态推送", DouYuTable.push)
    ), listOf(
        ManagerData.ExecButton("斗鱼鱼吧签到") { DouYuLogic.fishGroup(this as DouYuEntity) }
    )))
    it["kuGou"] = SignData("酷狗", listOf(
        SignData.LoginTypeButton("使用手机验证码登录", "kuGouLogin")
    ), ManagerData(KuGouEntity, KuGouTable.identityId, KuGouTable.identityName, listOf(
        ManagerData.ManagerButton("签到", KuGouTable.sign)
    ), listOf(
        ManagerData.ExecButton("酷狗概念版听歌得vip") { KuGouLogic.listenMusic(this as KuGouEntity) },
        ManagerData.ExecButton("酷狗概念版看广告") { KuGouLogic.watchAd(this as KuGouEntity) }
    )))
    it["miHoYo"] = SignData("米哈游", listOf(
        SignData.LoginTypeButton("使用cookie登录", "miHoYoCookieLogin"),
        SignData.LoginTypeButton("使用米游社APP扫码登录", "miHoYoQrcodeLogin"),
        SignData.LoginTypeButton("使用APP账号密码登录", "miHoYoAppPasswordLogin"),
        SignData.LoginTypeButton("使用WEB账号密码登录", "miHoYoWebPasswordLogin")
    ), ManagerData(MiHoYoEntity, MiHoYoTable.identityId, MiHoYoTable.identityName, listOf(
        ManagerData.ManagerButton("原神签到", MiHoYoTable.sign),
        ManagerData.ManagerButton("米游社签到", MiHoYoTable.mysSign)
    ), listOf(
        ManagerData.ExecButton("原神签到") { MiHoYoLogic.sign(this as MiHoYoEntity) },
        ManagerData.ExecButton("米游社签到") { MiHoYoLogic.mysSign(this as MiHoYoEntity) }
    )))
    it["step"] = SignData("刷步数", listOf(
        SignData.LoginTypeButton("小米运动账号密码登录", "xiaomiStepLogin"),
        SignData.LoginTypeButton("乐心运动账号密码登录", "leXinStepLogin")
    ), ManagerData(StepEntity, StepTable.identityId, StepTable.identityName, listOf(
        ManagerData.ManagerButton("步数偏移", StepTable.offset),
        ManagerData.ManagerButton("步数", null, "StepManagerStep", StepTable.step)
    ), listOf(
        ManagerData.ExecButton("刷步数", "请发送需要修改的步数") { any ->
            if ((this as StepEntity).leXinCookie != null)
                LeXinStepLogic.modifyStepCount(this, any!!.toString().toInt())
            if (this.miLoginToken != null)
                XiaomiStepLogic.modifyStepCount(this, any!!.toString().toInt())
        }
    )))
    it["weibo"] = SignData("微博", listOf(
        SignData.LoginTypeButton("使用微博APP扫码登录", "weiboQrcodeLogin")
    ), ManagerData(WeiboEntity, WeiboTable.identityId, WeiboTable.identityName, listOf(
        ManagerData.ManagerButton("签到", WeiboTable.sign),
        ManagerData.ManagerButton("微博推送", WeiboTable.push)
    ), listOf(
        ManagerData.ExecButton("微博超话签到") { WeiboLogic.superTalkSign(this as WeiboEntity) }
    )))
    it["smZdm"] = SignData("什么值得买", listOf(
        SignData.LoginTypeButton("使用手机验证码登录", "smZdmPhoneLogin"),
        SignData.LoginTypeButton("使用微信扫码登录", "smZdmWechatQrcodeLogin"),
        SignData.LoginTypeButton("使用APP扫码登录", "smZdmQrcodeLogin"),
        SignData.LoginTypeButton("使用cookie登录", "smZdmCookieLogin")
    ), ManagerData(SmZdmEntity, SmZdmTable.identityId, SmZdmTable.identityName, listOf(
        ManagerData.ManagerButton("签到", SmZdmTable.sign)
    ), listOf(
        ManagerData.ExecButton("什么值得买签到") {
            SmZdmLogic.webSign(this as SmZdmEntity)
            SmZdmLogic.appSign(this)
        }
    )))
    it["NodeSeek"] = SignData("NodeSeek", listOf(
        SignData.LoginTypeButton("使用cookie登录", "nodeSeekCookieLogin")
    ), ManagerData(NodeSeekEntity, NodeSeekTable.identityId, NodeSeekTable.identityName, listOf(
        ManagerData.ManagerButton("签到", null, "NodeSeekManagerSign", NodeSeekTable.sign)
    ), listOf(
        ManagerData.ExecButton("NodeSeek签到（随机）") { NodeSeekLogic.sign0(this as NodeSeekEntity, true) },
        ManagerData.ExecButton("NodeSeek签到（固定）") { NodeSeekLogic.sign0(this as NodeSeekEntity, false) }
    )))
    it["eCloud"] = SignData("天翼云盘", listOf(
        SignData.LoginTypeButton("使用密码登录", "eCloudPasswordLogin")
    ), ManagerData(ECloudEntity, ECloudTable.identityId, ECloudTable.identityName, listOf(
        ManagerData.ManagerButton("签到", ECloudTable.sign)
    ), listOf(
        ManagerData.ExecButton("天翼云盘签到") { ECloudLogic.sign(this as ECloudEntity) }
    )))
    it["hostLoc"] = SignData("HostLoc", listOf(
        SignData.LoginTypeButton("使用密码登录", "hostLocPasswordLogin"),
        SignData.LoginTypeButton("使用cookie登录", "hostLocCookieLogin")
    ), ManagerData(HostLocEntity, HostLocTable.identityId, HostLocTable.identityName, listOf(
        ManagerData.ManagerButton("签到", HostLocTable.sign)
    ), listOf(
        ManagerData.ExecButton("HostLoc签到") { HostLocLogic.sign(this as HostLocEntity) }
    )))
    it["huYa"] = SignData("虎牙", listOf(
        SignData.LoginTypeButton("使用虎牙APP扫码登录", "huYaQrcodeLogin")
    ), ManagerData(HuYaEntity, HuYaTable.identityId, HuYaTable.identityName, listOf(
        ManagerData.ManagerButton("开播提醒", HuYaTable.push)
    ), listOf()))
    it["NodeLoc"] = SignData("NodeLoc", listOf(
        SignData.LoginTypeButton("使用cookie登录", "nodeLocCookieLogin")
    ), ManagerData(NodeLocEntity, NodeLocTable.identityId, NodeLocTable.identityName, listOf(
        ManagerData.ManagerButton("签到", NodeLocTable.sign)
    ), listOf(
        ManagerData.ExecButton("NodeLoc签到") { NodeLocLogic.sign(this as NodeLocEntity) },
    )))
}

context(handing: BotHandling, container: Container)
private suspend fun TreeMap<String, SignData>.toMarkup(next: String): InlineKeyboardMarkup {
    val buttonFactory = getKoin().get<ButtonFactory>()
    val list = mutableListOf<List<InlineKeyboardButton>>()
    val iterator = this.entries.iterator()
    while (iterator.hasNext()) {
        val first = iterator.next()
        if (iterator.hasNext()) {
            val second = iterator.next()
            list.add(listOf(handing.callbackButton(container.chatId, container.fromId, first.value.text, next, first.key),
                handing.callbackButton(container.chatId, container.fromId, second.value.text, next, second.key)))
        } else {
            list.add(listOf(buttonFactory.callbackButton(first.value.text, first.key)))
        }
    }
    return InlineKeyboardMarkup(list)
}

private data class SignData(
    val text: String,
    val loginTypes: List<LoginTypeButton>,
    val managerData: ManagerData
) {
    data class LoginTypeButton(val text: String, val key: String)
}

private data class ManagerData(
    val entity: IntEntityClass<*>,
    val whereColumn: Column<String>,
    val whereColumn0: Column<String>,
    val button: List<ManagerButton>,
    val execButton: List<ExecButton>
) {
    val table: Table = entity.table

    data class ManagerButton(val text: String, val column: Column<Status>?,
                             val next: String? = null, val anotherColumn: Column<*>? = null)

    data class ExecButton(
        val text: String,
        val sendText: String? = null,
        val exec: suspend IntEntity.(Any?) -> Any? = {}
    )
}

@Factory
class IdentityHandler: BotHandler({

    suspend fun GeneralContainer.selectIdentity(key: String) {
        val identityList = suspendTransaction {
            IdentityEntity.find { IdentityTable.identityId eq fromId.toString() }.toList()
        }
        val name = loginButtonTreeMap[key]!!.text
        val list = mutableListOf<InlineKeyboardButton>()
        list.add(callbackButton("默认", "signSelectType", KeyAndIdentityName(key, "", false)))
        for (identityEntity in identityList) {
            list.add(callbackButton(identityEntity.name(), "signSelectType",
                KeyAndIdentityName(key, identityEntity.identityName, false)))
        }
        list.add(callbackButton("========", "noHandler"))
        list.add(callbackButton("新增", "identityAdd", key))
        list.add(callbackButton("管理", "identityManager", key))
        list.add(callbackButton("返回", "sign", key))
        if (this is CallbackContainer) {
            editMessageText(message.messageId, "$name\n请选择您的身份，用于多账号", replyMarkup = inlineKeyboard(*list.toTypedArray()))
        } else {
            sendMessage("$name\n请选择您的身份，用于多账号", replyMarkup = inlineKeyboard(*list.toTypedArray()))
        }
    }

    callback("selectIdentity") {
        val key = transferred<String>()
        selectIdentity(key)
    }

    callback("identityAdd") {
        editMessageText(message.messageId, "请发送您需要新增的身份名称")
        next("identityAdd0", transferred())
    }
    step("identityAdd0") {
        suspendTransaction {
            val find = IdentityEntity.find {
                IdentityTable.identityId eq fromId.toString() and
                        (IdentityTable.identityName eq text)
            }.firstOrNull()
            if (find != null) throw ChatException("该名称已存在，请重新发送名称")
            IdentityTable.insert {
                it[identityId] = fromId.toString()
                it[identityName] = text
            }
        }
        sendMessage("添加身份名称 $text 成功")
        selectIdentity(transferred())
    }

    callback("identityManager") {
        val identityList = suspendTransaction {
            IdentityEntity.find { IdentityTable.identityId eq fromId.toString() }.toList()
        }
        val key = transferred<String>()
        val list = identityList.map { callbackButton(it.name(), "identityManager0", KeyAndId(key, it.id.value)) }
            .toMutableList()
        list.add(callbackButton("返回", "selectIdentity", transferred()))
        editMessageText(message.messageId, "请选择您要操作的身份",
            replyMarkup = inlineKeyboard(*list.toTypedArray()))
    }

    callback("identityManager0") {
        val data = transferred<KeyAndId>()
        val name = suspendTransaction {
            IdentityEntity.find { IdentityTable.id eq data.id }.first()
        }.identityName
        editMessageText(message.messageId, "$name\n请选择您要操作方式", replyMarkup = inlineKeyboard(
            callbackButton("修改名称", "identityChangeName", data),
            callbackButton("删除", "identityDelete", data),
            callbackButton("返回", "identityManager", data.key)
        ))
    }

    callback("identityChangeName") {
        editMessageText(message.messageId, "请发送新的身份名称")
        next("identityChangeName0", transferred())
    }
    step("identityChangeName0") {
        val data = transferred<KeyAndId>()
        suspendTransaction {
            IdentityEntity.findByIdAndUpdate(data.id) {
                it.showName = text
            }
        }
        sendMessage("修改身份名称成功")
    }

    callback("identityDelete") {
        val data = transferred<KeyAndId>()
        suspendTransaction {
            val identityEntity = IdentityEntity.findById(data.id)!!
            for ((_, value) in loginButtonTreeMap) {
                val managerData = value.managerData
                managerData.entity.table.deleteWhere {
                    managerData.whereColumn eq identityEntity.identityId and
                            (managerData.whereColumn0 eq identityEntity.identityName)
                }
            }
            identityEntity.delete()
        }
        editMessageText(message.messageId, "删除身份成功", replyMarkup = inlineKeyboard(
            callbackButton("返回", "identityManager", data.key)
        ))
    }



})

private data class KeyAndIdentityName(val key: String, val identityName: String, val edit: Boolean = true)
private data class KeyAndId(val key: String, val id: Int)


@Factory
class LoginHandler: BotHandler({

    data class AccountAndIdentityName(val account: String, val keyAndIdentityName: KeyAndIdentityName)

    command("/sign") {
        sendMessage("请选择", replyMarkup = loginButtonTreeMap.toMarkup("selectIdentity"))
    }

    callback("sign") {
        editMessageText(message.messageId, "请选择", replyMarkup = loginButtonTreeMap.toMarkup("selectIdentity"))
    }

    suspend fun GeneralContainer.signSelectType(any: KeyAndIdentityName) {
        val key = any.key
        val identityName = any.identityName
        val button = loginButtonTreeMap[key]!!
        val newAny = KeyAndIdentityName(key, identityName)
        val markup = inlineKeyboard(
            callbackButton("登录", "signLogin", newAny),
            callbackButton("管理", "signManager", newAny),
            callbackButton("执行", "signExecute", newAny),
            callbackButton("删除", "signDelete", newAny),
//            callbackButton("返回", "selectIdentity", key),
        )
        val callbackContainer = this as? CallbackContainer
        if (any.edit) {
            callbackContainer?.let {
                editMessageText(it.message.messageId, "${button.text}\n身份：${identityName.default()}\n请选择", replyMarkup = markup)
            }

        }
        else {
            sendMessage("${button.text}\n身份：${identityName.default()}\n请选择", replyMarkup = markup)
            callbackContainer?.let {
                answerCallbackQuery(query.id, "获取成功")
            }
        }
    }

    callback("signSelectType") {
        val any = transferred<KeyAndIdentityName>()
        signSelectType(any)
    }

    callback("signLogin") {
        val any = transferred<KeyAndIdentityName>()
        val key = any.key
        val button = loginButtonTreeMap[key]!!
        val loginTypes = button.loginTypes
        val list = mutableListOf<InlineKeyboardButton>()
        loginTypes.forEach {
            list.add(callbackButton(it.text, it.key, any))
        }
        list.add(callbackButton("返回", "signSelectType", any))
        editMessageText(message.messageId, "${button.text}\n身份：${any.identityName.default()}\n请选择", replyMarkup = inlineKeyboard(*list.toTypedArray()))
        answerCallbackQuery(query.id, "获取成功")
    }

    suspend fun GeneralContainer.sendReturn(transferred: KeyAndIdentityName? = null) {
        val any = transferred ?: transferred<KeyAndIdentityName>()
        signSelectType(KeyAndIdentityName(any.key, any.identityName, false))
    }

    fun GeneralContainer.identityName(): String {
        return transferred<KeyAndIdentityName>().identityName
    }

   callback("baiduQrcodeLogin") {
       val qrcode = BaiduLogic.getQrcode()
       var photoMessage: Message?
       client.get(qrcode.image).body<ByteArray>().let {
           photoMessage = sendPhoto(ByteArrayContent(it))
           editMessageText(message.messageId, "请使用百度app扫描以下二维码登陆，百度网盘等均可")
       }
       var i = 0
       try {
           while (true) {
               if (++i > 20) {
                   deleteMessage(photoMessage!!.messageId)
                   error("百度二维码已超时")
               }
               delay(3000)
               try {
                   val newEntity = BaiduLogic.checkQrcode(qrcode)
                   suspendTransaction {
                       BaiduTable.upsert(BaiduTable.identityId, BaiduTable.identityName) {
                           it[identityId] = query.from.id.toString()
                           it[identityName] = identityName()
                           it[cookie]= newEntity.cookie
                       }
                   }
                   editMessageText(message.messageId, "绑定百度成功")
                   sendReturn()
               } catch (_: QrcodeScanException) {}
           }
       } finally {
           photoMessage?.let { deleteMessage(it.messageId) }
       }
   }

    callback("biliBiliQrcodeLogin") {
        val qrcode = BiliBiliLogic.loginByQr1()
        var photoMessage: Message?
        qrcode(qrcode.url).let {
            photoMessage = sendPhoto(ByteArrayContent(it))
            editMessageText(message.messageId, "请使用哔哩哔哩app扫描以下二维码登陆")
        }
        var i = 0
        while (true) {
            if (++i > 10) {
                editMessageText(message.messageId, "哔哩哔哩二维码已超时")
                break
            }
            delay(3000)
            val newEntity = try {
                BiliBiliLogic.loginByQr2(qrcode)
            } catch (_: QrcodeScanException) {
                continue
            }
            suspendTransaction {
                BiliBiliTable.upsert(BiliBiliTable.identityId, BiliBiliTable.identityName) {
                    it[identityId] = from.id.toString()
                    it[identityName] = identityName()
                    it[cookie] = newEntity.cookie
                    it[userid] = newEntity.userid
                    it[token] = newEntity.token
                }
            }
            editMessageText(message.messageId, "绑定哔哩哔哩成功")
            sendReturn()
            break
        }
        deleteMessage(photoMessage!!.messageId)
    }

    callback("douYuQrcodeLogin") {
        val qrcode = DouYuLogic.getQrcode()
        val imageUrl = qrcode.url
        var photoMessage: Message?
        qrcode(imageUrl).let {
            photoMessage = sendPhoto(ByteArrayContent(it))
            editMessageText(message.messageId, "请使用斗鱼app扫码二维码登录")
        }
        var i = 0
        while (true) {
            if (i++ > 20) {
                editMessageText(message.messageId, "斗鱼登录二维码已失效")
                break
            }
            delay(3000)
            val newEntity = try {
                DouYuLogic.checkQrcode(qrcode)
            } catch (_: QrcodeScanException) {
                continue
            }
            suspendTransaction {
                DouYuTable.upsert(DouYuTable.identityId, DouYuTable.identityName) {
                    it[identityId] = query.from.id.toString()
                    it[identityName] = identityName()
                    it[cookie] = newEntity.cookie
                }
            }
            editMessageText(message.messageId, "绑定斗鱼成功")
            sendReturn()
            break
        }
        photoMessage?.let {
            deleteMessage(it.messageId)
        }
    }
    data class KuGouData(
        val phone: String,
        val mid: String,
        val keyAndIdentityName: KeyAndIdentityName
    )
    callback("kuGouLogin") {
        editMessageText(message.messageId, "请发送酷狗登录的手机号")
        next("kuGouLogin1", transferred())
    }
    step("kuGouLogin1", TextMessageContainer::class) {
        val mid = KuGouLogic.mid()
        KuGouLogic.sendMobileCode(text, mid)
        sendMessage("请发送酷狗登录的验证码")
        next("kuGouLogin2", KuGouData(text, mid, transferred()))
    }
    step("kuGouLogin2", TextMessageContainer::class) {
        val data = transferred<KuGouData>()
        val any = data.keyAndIdentityName
        val saveMid = data.mid
        val newKuGouEntity = KuGouLogic.verifyCode(data.phone, text, saveMid)
        suspendTransaction {
            KuGouTable.upsert(KuGouTable.identityId, KuGouTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = any.identityName
                it[token] = newKuGouEntity.token
                it[userid] = newKuGouEntity.userid
                it[kuGoo] = newKuGouEntity.kuGoo
                it[mid] = saveMid
            }
        }
        sendMessage("绑定酷狗成功")
        sendReturn(any)
    }

    callback("miHoYoCookieLogin") {
        editMessageText(message.messageId, "请发送米哈游cookie")
        next("miHoYoCookieLogin1", transferred())
    }
    step("miHoYoCookieLogin1") {
        val saveTicket = RegexUtils.extract(text, "login_ticket", ";") ?: ""
        val accountId = RegexUtils.extract(text, " account_id", ";") ?: ""
        MiHoYoTable.upsert(MiHoYoTable.identityId) {
            it[identityId] = from.id.toString()
            it[identityName] = identityName()
            it[cookie] = text
            it[ticket] = saveTicket
            it[aid] = accountId
        }
        sendMessage("绑定米哈游成功")
        sendReturn()
    }

    callback("miHoYoQrcodeLogin") {
        val qrcode = MiHoYoLogic.qrcodeLogin1()
        var photoMessage: Message?
        qrcode(qrcode.url).let {
            photoMessage = sendPhoto(ByteArrayContent(it))
            editMessageText(message.messageId, "请使用米游社扫描下面二维码登录")
        }
        var i = 0
        try {
            while (true) {
                if (i++ > 20) {
                    editMessageText(message.messageId, "米游社二维码已过期")
                    break
                }
                delay(3000)
                val miHoYoEntity = try {
                    MiHoYoLogic.qrcodeLogin2(qrcode)
                } catch (_: QrcodeScanException) {
                    continue
                }
                suspendTransaction {
                    MiHoYoTable.upsert(MiHoYoTable.identityId, MiHoYoTable.identityName) {
                        it[identityId] = from.id.toString()
                        it[identityName] = identityName()
                        it[fix] = miHoYoEntity.fix
                        it[cookie] = miHoYoEntity.cookie!!
                        it[aid] = miHoYoEntity.aid
                        it[mid] = miHoYoEntity.mid
                    }
                }
                editMessageText(message.messageId, "绑定米哈游成功")
                sendReturn()
                break
            }
        } finally {
            photoMessage?.let {
                deleteMessage(it.messageId)
            }
        }
    }

    callback("miHoYoAppPasswordLogin") {
        editMessageText(message.messageId, "请发送米哈游账号")
        next("miHoYoAppPasswordLogin1", transferred())
    }
    step("miHoYoAppPasswordLogin1") {
        sendMessage("请发送米哈游密码")
        next("miHoYoAppPasswordLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("miHoYoAppPasswordLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val entity = MiHoYoLogic.login(account, password)
        suspendTransaction {
            MiHoYoTable.upsert(MiHoYoTable.identityId, MiHoYoTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[aid] = entity.aid
                it[mid] = entity.mid
                it[token] = entity.token
                it[ticket] = entity.ticket!!
                it[fix] = entity.fix
                it[sToken] = entity.sToken!!
            }
        }
        sendMessage("绑定米哈游成功")
        sendReturn(keyAndIdentityName)
    }

    callback("miHoYoWebPasswordLogin") {
        editMessageText(message.messageId, "请发送米哈游账号")
        next("miHoYoWebPasswordLogin1", transferred())
    }
    step("miHoYoWebPasswordLogin1") {
        sendMessage("请发送米哈游密码")
        next("miHoYoWebPasswordLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("miHoYoWebPasswordLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val entity = MiHoYoLogic.webLogin(account, password)
        suspendTransaction {
            MiHoYoTable.upsert(MiHoYoTable.identityId, MiHoYoTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[aid] = entity.aid
                it[mid] = entity.mid
                it[fix] = entity.fix
                it[cookie] = entity.cookie!!
            }
        }
        sendMessage("绑定米哈游成功")
        sendReturn(keyAndIdentityName)
    }

    callback("weiboQrcodeLogin") {
        val qrcode = WeiboLogic.login1()
        val photoMessage: Message?
        client.get(qrcode.image).body<ByteArray>().let {
            photoMessage = sendPhoto(ByteArrayContent(it))
            editMessageText(message.messageId, "使用微博app扫码登陆")
        }
        var i = 0
        var fail = true
        while (true) {
            if (i++ > 20) break
            delay(3000)
            try {
                val newEntity = WeiboLogic.login2(qrcode)
                suspendTransaction {
                    WeiboTable.upsert(WeiboTable.identityId, WeiboTable.identityName) {
                        it[identityId] = from.id.toString()
                        it[identityName] = identityName()
                        it[cookie] = newEntity.cookie
                    }
                }
                editMessageText(message.messageId, "绑定微博成功")
                sendReturn()
                fail = false
                break
            } catch (_: QrcodeScanException) {
                continue
            }
        }
        photoMessage?.let {
            deleteMessage(it.messageId)
        }
        if (fail) {
            editMessageText(message.messageId, "微博二维码已过期")
        }
    }

    callback("smZdmCookieLogin") {
        editMessageText(message.messageId, "请发送什么值得买cookie")
        next("smZdmCookieLogin1", transferred())
    }
    step("smZdmCookieLogin1") {
        suspendTransaction {
            SmZdmTable.upsert(SmZdmTable.identityId, SmZdmTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = identityName()
                it[cookie] = text
            }
        }
        sendMessage("绑定什么值得买成功")
        sendReturn()
    }

    callback("nodeSeekCookieLogin") {
        editMessageText(message.messageId, "请发送NodeSeek的cookie")
        next("nodeSeekCookieLogin1", transferred())
    }
    step("nodeSeekCookieLogin1") {
        if (!text.contains("session=") || !text.contains("cf_clearance=")) {
            throw ChatException("cookie格式错误，您发送的cookie有误，需包含session=和cf_clearance=等，请重新发送")
        }
        suspendTransaction {
            NodeSeekTable.upsert(NodeSeekTable.identityId, NodeSeekTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = identityName()
                it[cookie] = text
            }
        }
        sendMessage("绑定NodeSeek成功")
        sendReturn()
    }

    callback("eCloudPasswordLogin") {
        editMessageText(message.messageId, "请发送天翼云盘账号")
        next("eCloudPasswordLogin1", transferred())
    }
    step("eCloudPasswordLogin1") {
        sendMessage("请发送天翼云盘密码")
        next("eCloudPasswordLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("eCloudPasswordLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val entity = ECloudLogic.login(account, password)
        suspendTransaction {
            ECloudTable.upsert(ECloudTable.identityId, ECloudTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[cookie] = entity.cookie
                it[eCookie] = entity.eCookie
            }
        }
        sendMessage("绑定天翼云盘成功")
        sendReturn(keyAndIdentityName)
    }

    callback("hostLocPasswordLogin") {
        editMessageText(message.messageId, "请发送HostLoc账号")
        next("hostLocPasswordLogin1", transferred())
    }
    step("hostLocPasswordLogin1") {
        sendMessage("请发送HostLoc密码")
        next("hostLocPasswordLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("hostLocPasswordLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val resultCookie = HostLocLogic.login(account, password)
        suspendTransaction {
            HostLocTable.upsert(HostLocTable.identityId, HostLocTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[cookie] = resultCookie
            }
        }
        sendMessage("绑定HostLoc成功")
        sendReturn(keyAndIdentityName)
    }

    callback("hostLocCookieLogin") {
        editMessageText(message.messageId, "请发送HostLoc的cookie")
        next("hostLocCookieLogin1", transferred())
    }
    step("hostLocCookieLogin1") {
        suspendTransaction {
            HostLocTable.upsert(HostLocTable.identityId, HostLocTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = identityName()
                it[cookie] = text
            }
        }
        sendMessage("绑定HostLoc成功")
        sendReturn()
    }

    callback("huYaQrcodeLogin") {
        val qrcode = HuYaLogic.getQrcode()
        val photoMessage: Message?
        client.get(qrcode.url).body<ByteArray>().let {
            photoMessage = sendPhoto(ByteArrayContent(it))
            editMessageText(message.messageId, "请使用虎牙App扫描二维码登录")
        }
        var i = 0
        while (true) {
            if (i++ > 20) {
                editMessageText(message.messageId, "虎牙登录二维码已过期")
                break
            }
            delay(3000)
            val newEntity = try {
                HuYaLogic.checkQrcode(qrcode)
            } catch (_: QrcodeScanException) {
                continue
            }
            suspendTransaction {
                HuYaTable.upsert(HuYaTable.identityId, HuYaTable.identityName) {
                    it[identityId] = from.id.toString()
                    it[identityName] = identityName()
                    it[cookie] = newEntity.cookie
                }
            }
            editMessageText(message.messageId, "绑定虎牙成功")
            sendReturn()
            break
        }
        photoMessage?.let {
            deleteMessage(it.messageId)
        }
    }

    callback("xiaomiStepLogin") {
        editMessageText(message.messageId, "请发送小米运动账号")
        next("xiaomiStepLogin1", transferred())
    }
    step("xiaomiStepLogin1") {
        sendMessage("请发送小米运动密码")
        next("xiaomiStepLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("xiaomiStepLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val entity = XiaomiStepLogic.login(account, password)
        suspendTransaction {
            StepTable.upsert(StepTable.identityId, StepTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[miLoginToken] = entity.miLoginToken
            }
        }
        sendMessage("绑定小米运动成功")
        sendReturn(keyAndIdentityName)
    }

    callback("leXinStepLogin") {
        editMessageText(message.messageId, "请发送乐心运动账号")
        next("leXinStepLogin1", transferred())
    }
    step("leXinStepLogin1") {
        sendMessage("请发送乐心运动密码")
        next("leXinStepLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("leXinStepLogin2") {
        val password = text
        val any = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = any.keyAndIdentityName
        val account = any.account
        val entity = LeXinStepLogic.login(account, password)
        suspendTransaction {
            StepTable.upsert(StepTable.identityId, StepTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[leXinCookie] = entity.leiXinCookie
                it[leXinUserid] = entity.leXinUserid
                it[leXinAccessToken] = entity.leiXinAccessToken
            }
        }
        sendMessage("绑定乐心运动成功")
        sendReturn(keyAndIdentityName)
    }


    callback("nodeLocCookieLogin") {
        editMessageText(message.messageId, "请发送NodeLoc的cookie")
        next("nodeLocCookieLogin1", transferred())
    }
    step("nodeLocCookieLogin1") {
        sendMessage("请发送NodeLoc的x-csrf-token")
        next("nodeLocCookieLogin2", AccountAndIdentityName(text, transferred()))
    }
    step("nodeLocCookieLogin2") {
        val transferred = transferred<AccountAndIdentityName>()
        val keyAndIdentityName = transferred.keyAndIdentityName
        suspendTransaction {
            NodeLocTable.upsert(NodeLocTable.identityId, NodeLocTable.identityName) {
                it[identityId] = from.id.toString()
                it[identityName] = keyAndIdentityName.identityName
                it[cookie] = transferred.account
                it[csrf] = text
            }
        }
        sendMessage("绑定NodeLoc成功")
        sendReturn(keyAndIdentityName)
    }

})

private data class ManagerTransferData(
    val keyAndIdentityName: KeyAndIdentityName,
    val buttonText: String
)

@Factory
class ManagerHandler: BotHandler({

    suspend fun managerMarkup(keyAndIdentityName: KeyAndIdentityName, chatId: Long, fromId: Long): List<List<InlineKeyboardButton>> {
        val buttonFactory = getKoin().get<ButtonFactory>()
        val key = keyAndIdentityName.key
        val identityName = keyAndIdentityName.identityName
        val signData = loginButtonTreeMap[key]!!
        val managerData = signData.managerData
        val table = managerData.table
        val button = managerData.button
        val filterButton = button.filter { it.column != null }
        val list = mutableListOf<List<InlineKeyboardButton>>()
        if (filterButton.isNotEmpty()) {
            val resultStatus = suspendTransaction {
                table.select(filterButton.map { it.column!! })
                    .andWhere { managerData.whereColumn eq fromId.toString() }
                    .andWhere { managerData.whereColumn0 eq identityName }
                    .toList()
            }[0]
            filterButton.forEach {
                list.add(
                    listOf(
                        buttonFactory.callbackButton(
                            chatId, fromId,
                            "${it.text}  ${resultStatus[it.column!!]}",
                            "signManagerExecute",
                            ManagerTransferData(keyAndIdentityName, it.text)
                        )
                    )
                )
            }
        }
        val anotherButton = button.filter { it.anotherColumn != null }
        if (anotherButton.isNotEmpty()) {
            val queryResult = suspendTransaction {
                table.select(anotherButton.map { it.anotherColumn!! })
                    .andWhere { managerData.whereColumn eq fromId.toString() }
                    .andWhere { managerData.whereColumn0 eq identityName }
                    .toList()
            }[0]
            anotherButton.forEach {
                list.add(
                    listOf(
                        buttonFactory.callbackButton(
                            chatId, fromId,
                            "${it.text}  ${queryResult[it.anotherColumn!!]}",
                            it.next!!,
                            ManagerTransferData(keyAndIdentityName, it.text)
                        )
                    )
                )
            }

        }
        list.add(listOf(buttonFactory.callbackButton(chatId, fromId, "返回", "signSelectType", keyAndIdentityName)))
        return list
    }

    callback("signManager") {
        val any = transferred<KeyAndIdentityName>()
        val key = any.key
        val identityName = any.identityName
        val signData = loginButtonTreeMap[key]!!
        val text = signData.text
        val managerData = signData.managerData
        val table = managerData.table
        suspendTransaction {
            table.selectAll()
                .andWhere { managerData.whereColumn eq query.from.id.toString() }
                .andWhere { managerData.whereColumn0 eq identityName }
                .toList()
        }.takeIf { it.isEmpty() }?.let {
            answerCallbackQuery(query.id, "未绑定${text}")
            return@callback
        }

        val list = managerMarkup(any, chatId, fromId)

        editMessageText(message.messageId, "${text}管理\n身份：${identityName.default()}", replyMarkup = InlineKeyboardMarkup(list))
    }

    callback("signManagerExecute") {
        val data = transferred<ManagerTransferData>()
        val any = data.keyAndIdentityName
        val key = any.key
        val signData = loginButtonTreeMap[key]!!
        val managerData = signData.managerData
        val table = managerData.table
        val button = managerData.button
        val singleButton = button.find { it.text == data.buttonText }!!
        val column = singleButton.column!!
        suspendTransaction {
            val status = table.select(column)
                .andWhere { managerData.whereColumn eq fromId.toString() }
                .andWhere { managerData.whereColumn0 eq any.identityName }
                .first()[column]
            table.update({ (managerData.whereColumn eq fromId.toString()) and (managerData.whereColumn0 eq any.identityName) }) {
                it[column] = !status
            }
        }

        val list = managerMarkup(any, chatId, fromId)

        editMessageText(message.messageId, "${signData.text}管理", replyMarkup = InlineKeyboardMarkup(list))

    }

    callback("NodeSeekManagerSign") {
        val data = transferred<ManagerTransferData>()
        val any = data.keyAndIdentityName
        val key = any.key
        val signData = loginButtonTreeMap[key]!!
        suspendTransaction {
            val entity = NodeSeekEntity.find {
                NodeSeekTable.identityId eq fromId.toString() and
                        (NodeSeekTable.identityName eq any.identityName)
            }.first()
            entity.sign = !entity.sign
        }
        val list = managerMarkup(any, chatId, fromId)
        editMessageText(message.messageId, "${signData.text}管理", replyMarkup = InlineKeyboardMarkup(list))
    }

    callback("StepManagerStep") {
        editMessageText(message.messageId, "请发送需要定时修改的步数")
        next("StepManagerStep1", transferred<ManagerTransferData>())
    }
    step("StepManagerStep1") {
        val modifyStep = text.toIntOrNull() ?: error("")
        val data = transferred<ManagerTransferData>()
        val any = data.keyAndIdentityName
        val key = any.key
        val signData = loginButtonTreeMap[key]!!
        suspendTransaction {
            StepTable.update({ StepTable.identityId eq fromId.toString() and (StepTable.identityName eq any.identityName ) }) {
                it[step] = modifyStep
            }
        }
        val list = managerMarkup(any, chatId, fromId)
        sendMessage("${signData.text}管理", replyMarkup = InlineKeyboardMarkup(list))
    }

})

@Factory
class DeleteHandler: BotHandler({
    callback("signDelete") {
        val any = transferred<KeyAndIdentityName>()
        val signData = loginButtonTreeMap[any.key]!!
        val text = signData.text
        editMessageText(message.messageId, "确认要删除${text}吗？", replyMarkup = inlineKeyboard(
            callbackButton("是", "signDelete0", any),
            callbackButton("否", "signSelectType", any)
        ))
    }
    callback("signDelete0") {
        val any = transferred<KeyAndIdentityName>()
        val signData = loginButtonTreeMap[any.key]!!
        val text = signData.text
        val managerData = signData.managerData
        val table = managerData.table
        suspendTransaction {
            table.deleteWhere { managerData.whereColumn eq fromId.toString() and (managerData.whereColumn0 eq any.identityName) }
        }
        editMessageText(message.messageId, "删除${text}成功", replyMarkup = inlineKeyboard(
            callbackButton("返回", "signSelectType", any)
        ))
    }
})

@Factory
class ExecuteHandler: BotHandler({

    callback("signExecute") {
        val any = transferred<KeyAndIdentityName>()
        val signData = loginButtonTreeMap[any.key]!!
        val text = signData.text
        val managerData = signData.managerData
        val table = managerData.table
        val execButton = managerData.execButton
        suspendTransaction {
            table.selectAll()
                .andWhere { managerData.whereColumn eq fromId.toString() }
                .andWhere { managerData.whereColumn0 eq any.identityName }
                .toList()
        }.takeIf { it.isEmpty() }?.let {
            answerCallbackQuery(query.id, "未绑定${text}")
            return@callback
        }
        val list = mutableListOf<List<InlineKeyboardButton>>()
        execButton.forEach {
            list.add(listOf(callbackButton(it.text, "signExecute0", ManagerTransferData(any, it.text))))
        }
        list.add(listOf(callbackButton("返回", "signSelectType", any)))
        editMessageText(message.messageId, "${text}执行\n身份：${any.identityName.default()}", replyMarkup = InlineKeyboardMarkup(list))
        answerCallbackQuery(query.id, "获取成功")
    }

    callback("signExecute0") {
        val data = transferred<ManagerTransferData>()
        val any = data.keyAndIdentityName
        val key = any.key
        val signData = loginButtonTreeMap[key]!!
        val managerData = signData.managerData
        val entity = managerData.entity
        val button = managerData.execButton
        val singleButton = button.find { it.text == data.buttonText }!!
        val sendText = singleButton.sendText
        if (sendText != null) {
            editMessageText(message.messageId, sendText)
            return@callback next("signExecute1", data)
        }
        editMessageText(message.messageId, "${singleButton.text}执行中，请稍后...")
        val exec = singleButton.exec
        val queryEntity = suspendTransaction {
            entity.find { managerData.whereColumn eq fromId.toString() and (managerData.whereColumn0 eq any.identityName) }.first()
        }
        val returnButton = inlineKeyboard(callbackButton("返回", "signExecute", any))
        try {
            val execResult = exec(queryEntity, null)
            if (execResult is String) {
                editMessageText(message.messageId, execResult, replyMarkup = returnButton)
            } else {
                editMessageText(message.messageId, "${singleButton.text}执行成功", replyMarkup = returnButton)
            }
        } catch (e: Exception) {
            if (e !is IllegalStateException) {
                e.printStackTrace()
            }
            editMessageText(message.messageId, "${singleButton.text}执行失败，异常信息：${e.message}", replyMarkup = returnButton)
        }
    }

    step("signExecute1") {
        val data = transferred<ManagerTransferData>()
        val any = data.keyAndIdentityName
        val key = any.key
        val signData = loginButtonTreeMap[key]!!
        val managerData = signData.managerData
        val entity = managerData.entity
        val button = managerData.execButton
        val singleButton = button.find { it.text == data.buttonText }!!
        val exec = singleButton.exec
        val queryEntity = suspendTransaction {
            entity.find { managerData.whereColumn eq fromId.toString() and (managerData.whereColumn0 eq any.identityName) }.first()
        }
        val returnButton = inlineKeyboard(callbackButton("返回", "signExecute", any))
        try {
            exec(queryEntity, text)
            sendMessage("${singleButton.text}执行成功", replyMarkup = returnButton)
        } catch (e: Exception) {
            sendMessage("${singleButton.text}执行失败，异常信息：${e.message}", replyMarkup = returnButton)
        }
    }

})

private fun String.default(): String {
    return this.ifEmpty { "默认" }
}
