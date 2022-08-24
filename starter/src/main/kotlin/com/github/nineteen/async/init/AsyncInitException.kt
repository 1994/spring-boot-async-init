package com.github.nineteen.async.init

import java.lang.RuntimeException

open class AsyncInitException(extMap: Map<String, Throwable>) : RuntimeException(exToString(extMap))

open class AsyncConfigException(asyncInitConfig: AsyncInitConfig, message: String): RuntimeException(
    """
        please check your async config, message:${message}, config:${asyncInitConfig}
    """.trimIndent()
)
fun exToString(extMap: Map<String, Throwable>) : String {
    return """
        ${extMap.size} beans async init error, detail:
        ${extMap.map { 
            "beanName: ${it.key} invoke init message error, message:${it.value.stackTraceToString()}\n"
    }}
    """.trimIndent()
}