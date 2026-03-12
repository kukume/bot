package me.kuku.qqbot.context

import io.ktor.server.config.*
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin
import kotlin.reflect.KFunction
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.jvm.kotlinFunction

private typealias GroupMessageContextBody = GroupMessageContext.() -> Unit
private typealias PrivateMessageContextBody = PrivateMessageContext.() -> Unit
private typealias MessageContextBody = MessageContext.() -> Unit

class Subscribe: KoinComponent {

    private var init = false

    private val groupContextBody = mutableListOf<GroupMessageContextBody>()
    private val privateContextBody = mutableListOf<PrivateMessageContextBody>()
    private val messageContextBody = mutableListOf<MessageContextBody>()
    val groupMessageContext = GroupMessageContext()
    val privateMessageContext = PrivateMessageContext()
    val messageContext = MessageContext()

    fun init() {
        if (init) error("already init")
        init = true
        val functionList = scan("subscribe")
        functionList.forEach { it.call(this) }
        messageContextBody.forEach {
            it.invoke(messageContext)
        }
        groupContextBody.forEach {
            it.invoke(groupMessageContext)
        }
        privateContextBody.forEach {
            it.invoke(privateMessageContext)
        }
    }

    fun group(block: GroupMessageContextBody) {
        groupContextBody.add(block)
    }

    fun private(block: PrivateMessageContextBody) {
        privateContextBody.add(block)
    }

    fun all(block: MessageContextBody) {
        messageContextBody.add(block)
    }

}

fun scan(path: String): List<KFunction<*>> {
    val functionList = mutableListOf<KFunction<*>>()
    val subscribeList = getKoin().get<ApplicationConfig>(named("qqConfig")).property(path).getList()
    for (name in subscribeList) {
        val className = name.substringBeforeLast(".")
        val functionName = name.substringAfterLast(".")
        val clazz = Class.forName(className)
        clazz.declaredMethods.filter { it.name == functionName }.forEach { method ->
            method.kotlinFunction?.let {
                functionList += it
            }
        }
    }
    return functionList.filter { it.extensionReceiverParameter != null }
}