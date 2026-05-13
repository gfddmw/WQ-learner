package com.example.wq_learner1.auth

import android.app.Activity
import android.content.Context
import android.graphics.Color
import java.lang.reflect.Proxy

interface PhoneAuthGateway {
    fun requestLoginToken(
        activity: Activity,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )
}

class AliyunPhoneAuthGateway(
    private val authSecret: String,
    private val timeoutMs: Int = 5_000,
) : PhoneAuthGateway {
    override fun requestLoginToken(
        activity: Activity,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (authSecret.isBlank()) {
            onError("未配置阿里云号码认证方案密钥")
            return
        }

        try {
            val listenerClass = Class.forName("com.mobile.auth.gatewayauth.TokenResultListener")
            val helperClass = Class.forName("com.mobile.auth.gatewayauth.PhoneNumberAuthHelper")
            var listenerRef: Any? = null
            val listener = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass),
            ) { _, method, args ->
                val payload = args?.firstOrNull()?.toString().orEmpty()
                when (method.name) {
                    "onTokenSuccess" -> {
                        val token = payload.extractAliyunAccessToken()
                        if (token != null) {
                            listenerRef?.let { proxy ->
                                quitLoginPage(helperClass, activity.applicationContext, listenerClass, proxy)
                            }
                            onSuccess(token)
                        }
                    }
                    "onTokenFailed" -> onError(payload.toAliyunFailureMessage())
                }
                null
            }
            listenerRef = listener

            val helper = helperClass
                .getMethod("getInstance", Context::class.java, listenerClass)
                .invoke(null, activity.applicationContext, listener)
                ?: throw IllegalStateException("号码认证 SDK 初始化失败")
            helperClass.getMethod("setAuthSDKInfo", String::class.java).invoke(helper, authSecret)
            runCatching {
                helperClass.getMethod("setAuthListener", listenerClass).invoke(helper, listener)
            }
            configureAuthUi(helperClass, helper)
            helperClass
                .getMethod("getLoginToken", Context::class.java, Int::class.javaPrimitiveType!!)
                .invoke(helper, activity, timeoutMs)
        } catch (error: ClassNotFoundException) {
            onError("尚未导入阿里云号码认证 SDK AAR")
        } catch (error: ReflectiveOperationException) {
            onError(error.message ?: "号码认证 SDK 调用失败")
        } catch (error: RuntimeException) {
            onError(error.message ?: "号码认证失败")
        }
    }

    private fun quitLoginPage(
        helperClass: Class<*>,
        context: Context,
        listenerClass: Class<*>,
        listener: Any,
    ) {
        runCatching {
            val helper = helperClass.getMethod("getInstance", Context::class.java, listenerClass).invoke(null, context, listener)
            helperClass.getMethod("quitLoginPage").invoke(helper)
        }
    }

    private fun configureAuthUi(helperClass: Class<*>, helper: Any) {
        runCatching {
            val configClass = Class.forName("com.mobile.auth.gatewayauth.AuthUIConfig")
            val builderClass = Class.forName("com.mobile.auth.gatewayauth.AuthUIConfig\$Builder")
            val builder = builderClass.getDeclaredConstructor().newInstance()
            builderClass.getMethod("setAppPrivacyOne", String::class.java, String::class.java)
                .invoke(builder, "《隐私政策》", "https://www.aliyun.com")
            builderClass.getMethod("setAppPrivacyColor", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(builder, Color.GRAY, Color.rgb(47, 126, 104))
            builderClass.getMethod("setSwitchAccHidden", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
            builderClass.getMethod("setLogBtnToastHidden", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
            builderClass.getMethod("setNavHidden", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
            val config = builderClass.getMethod("create").invoke(builder)
            helperClass.getMethod("setAuthUIConfig", configClass).invoke(helper, config)
        }
    }
}

private fun String.extractAliyunAccessToken(): String? {
    val tokenKeys = listOf("token", "accessToken", "access_token")
    tokenKeys.forEach { key ->
        val token = jsonValue(key)
        if (token.isNotBlank()) {
            return token
        }
    }
    return takeIf { isNotBlank() && !trimStart().startsWith("{") }
}

internal fun String.toAliyunFailureMessage(): String {
    val message = jsonValue("msg").ifBlank { jsonValue("message") }
    val code = jsonValue("code")
    return when {
        message.isNotBlank() && code.isNotBlank() -> "号码认证失败：$message（$code）"
        message.isNotBlank() -> "号码认证失败：$message"
        isNotBlank() -> "号码认证失败：$this"
        else -> "号码认证失败"
    }
}

private fun String.jsonValue(key: String): String {
    val pattern = Regex(""""$key"\s*:\s*"((?:\\.|[^"])*)"""", RegexOption.IGNORE_CASE)
    return pattern.find(this)?.groupValues?.get(1)?.jsonUnescape().orEmpty()
}

private fun String.jsonUnescape(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char != '\\' || index == lastIndex) {
            result.append(char)
            index += 1
            continue
        }
        when (val escaped = this[index + 1]) {
            '"' -> result.append('"')
            '\\' -> result.append('\\')
            '/' -> result.append('/')
            'b' -> result.append('\b')
            'f' -> result.append('\u000C')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            't' -> result.append('\t')
            else -> result.append(escaped)
        }
        index += 2
    }
    return result.toString()
}
