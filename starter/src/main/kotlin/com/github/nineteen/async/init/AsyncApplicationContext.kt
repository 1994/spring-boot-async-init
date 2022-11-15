package com.github.nineteen.async.init

import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.util.StopWatch
import javax.sql.DataSource

const val CONFIG_PREFIX = "spring.async.init"

@ConfigurationProperties(prefix = CONFIG_PREFIX)
data class AsyncInitConfig(
    var switch: Boolean = false,
    var postConstructSwitch: Boolean = false,
    var syncBeanKeywords: Set<String>? = setOf("org.springframework"),
    var highOrderBeanClass: Set<Class<*>> = setOf(DataSource::class.java),
    var asyncBasePackage: Set<String>? = null
) {
    fun isAsyncBean(beanName: String, bean: Any): Boolean {
        if (!switch) {
            return false
        }
        if (isSyncBean(beanName, bean)) {
            return false
        }
        return asyncBasePackage?.any {
            bean.javaClass.canonicalName.startsWith(it)
        } == true
    }

    fun isSyncBean(beanName: String, bean: Any): Boolean {
        if (!switch) {
            return true
        }

        val canonicalName = bean.javaClass.canonicalName
        return syncBeanKeywords?.any {
            beanName.contains(it, ignoreCase = true) || canonicalName.contains(
                it,
                ignoreCase = true
            )
        } == true
    }
}

class WebServletAsyncApplicationContext : AnnotationConfigServletWebServerApplicationContext(AsyncBeanFactory()) {
    override fun setEnvironment(environment: ConfigurableEnvironment) {
        super.setEnvironment(environment)
        addEnv()
    }

    override fun finishRefresh() {
        asyncAwait()
        super.finishRefresh()
    }
}

class WebReactiveAsyncApplicationContext : AnnotationConfigReactiveWebApplicationContext(AsyncBeanFactory()) {
    override fun setEnvironment(environment: ConfigurableEnvironment) {
        super.setEnvironment(environment)
        addEnv()
    }

    override fun finishRefresh() {
        asyncAwait()
        super.finishRefresh()
    }
}

class AsyncApplicationContext : AnnotationConfigApplicationContext(AsyncBeanFactory()) {

    override fun setEnvironment(environment: ConfigurableEnvironment) {
        super.setEnvironment(environment)
        addEnv()
    }

    override fun finishRefresh() {
        asyncAwait()
        super.finishRefresh()
    }
}

class AsyncBeanFactory : DefaultListableBeanFactory() {

    lateinit var environment: ConfigurableEnvironment

    override fun getBeanPostProcessors(): MutableList<BeanPostProcessor> {
        val postProcessors = super.getBeanPostProcessors()
        val asyncConfig = environment.getAsyncConfig()
        if (!asyncConfig.switch || !asyncConfig.postConstructSwitch) {
            return postProcessors
        }

        val doNotReplace = postProcessors.all { it !is InitDestroyAnnotationBeanPostProcessor }
        if (doNotReplace) {
           return postProcessors
        }

        val toMutableList = postProcessors.map {
            if (it is CommonAnnotationBeanPostProcessor) AsyncInitAnnotationBeanPostProcessor(
                asyncConfig
            ) else it
        }
            .toMutableList()
        return toMutableList
    }

    override fun invokeInitMethods(beanName: String, bean: Any, mbd: RootBeanDefinition?) {
        val method = { super.invokeInitMethods(beanName, bean, mbd) }
        if (isSyncBean(beanName, bean, mbd)) {
            method.invoke()
        } else {
            if (isAsyncBean(beanName, bean, mbd)) {
                logger.info("beanName:${beanName} add to async init pool")
                AsyncInitManager.submit(beanName, method)
            } else {
                logger.info("beanName:${beanName} unknown source, sync init")
                method.invoke()
            }
        }
    }
}

fun ConfigurableEnvironment.getAsyncConfig(): AsyncInitConfig {
    val asyncInitConfig = AsyncInitConfig()
    val switch = getProperty("${CONFIG_PREFIX}.switch", Boolean::class.java)
    if (switch != null) {
        asyncInitConfig.switch = switch
    }

    if (switch == true) {
        val basePackages = this.getProperty("${CONFIG_PREFIX}.basePackage", Set::class.java) as Set<String>?
        if (basePackages?.isEmpty() == true) {
            throw AsyncConfigException(asyncInitConfig, "basePackage is empty")
        }
        asyncInitConfig.asyncBasePackage = basePackages
    }

    val post = this.getProperty("${CONFIG_PREFIX}.postConstructSwitch", Boolean::class.java)
    if (post != null) {
        asyncInitConfig.postConstructSwitch = post
    }
    val keywords: Set<String>? = this.getProperty("${CONFIG_PREFIX}.syncBeanKeywords", Set::class.java) as Set<String>?
    if (keywords?.isNotEmpty() == true) {
        asyncInitConfig.syncBeanKeywords = keywords
    }

    return asyncInitConfig
}

fun GenericApplicationContext.addEnv() {
    if (beanFactory is AsyncBeanFactory) {
        (beanFactory as AsyncBeanFactory).environment = environment
    }
}

fun GenericApplicationContext.asyncAwait() {
    val asyncConfig = environment.getAsyncConfig()
    if (!asyncConfig.switch) {
        return
    }

    val stopWatch = StopWatch().apply { this.start() }
    AsyncInitManager.await()
    stopWatch.stop()
//    logger.info("async init manager await cost: ${stopWatch.totalTimeMillis}ms")
}


fun AsyncBeanFactory.isSyncBean(beanName: String, bean: Any, mbd: RootBeanDefinition?): Boolean {
    val asyncConfig = this.environment.getAsyncConfig()
    if (!asyncConfig.switch) {
        return true
    }
    val beanClassName = mbd?.beanClassName
    val factoryName = mbd?.factoryBeanName

    return asyncConfig.syncBeanKeywords?.any {
        beanName.contains(it, ignoreCase = true) || beanClassName?.contains(
            it, ignoreCase = true
        ) == true || factoryName?.contains(it, ignoreCase = true) == true
    } == true
}

fun AsyncBeanFactory.isAsyncBean(beanName: String, bean: Any, mbd: RootBeanDefinition?): Boolean {
    val asyncConfig = environment.getAsyncConfig()
    return asyncConfig.isAsyncBean(beanName, bean)
}

class AsyncInitAnnotationBeanPostProcessor(private val asyncInitConfig: AsyncInitConfig) :
    CommonAnnotationBeanPostProcessor() {
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val method = {
            val postProcessBeforeInitialization = super.postProcessBeforeInitialization(bean, beanName)
        }

        if (asyncInitConfig.isAsyncBean(beanName, bean)) {
            logger.info("bean:${beanName} postConstruct async init")
            AsyncInitManager.submit(beanName, method)
            return bean
        } else {
            return super.postProcessBeforeInitialization(bean, beanName)
        }
    }
}