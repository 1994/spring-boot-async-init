package com.github.nineteen.async.init

import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


object AsyncInitManager {

    private val exMap: MutableMap<String, Throwable> = mutableMapOf()

    private val totalFuture: MutableList<CompletableFuture<*>> = mutableListOf()

    private val executor = AsyncManagerPool().apply { prestartCoreThread() }

    fun await() {
        executor.shutdown()
        totalFuture.map { it.join() }
        if (exMap.isNotEmpty()) {
            throw AsyncInitException(exMap)
        }
    }

    fun submit(beanName: String, run: () -> Unit) {
        val future = CompletableFuture.runAsync(run, executor).exceptionally {
            exMap[beanName] = it
            null
        }
        totalFuture.add(future)
    }
}

class AsyncManagerPool(number: Int = Runtime.getRuntime().availableProcessors()) : ThreadPoolExecutor(
    number, number, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
) {
    override fun beforeExecute(t: Thread?, r: Runnable?) {
        super.beforeExecute(t, r)
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
    }
}