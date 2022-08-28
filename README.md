# spring-boot-async-init

spring bean默认是单线程同步初始化的，如果你的应用中含有大量初始化耗时的bean，会造成启动特别慢。
使用这个starter可以支持bean异步初始化，从而加速启动spring boot应用

## 使用限制

并不是所有的bean是适合异步初始化的，以下两类bean是没有必要异步初始化：

1. 初始化快的bean，初始化过程中没有耗时的逻辑
2. 初始化逻辑存在相互依赖的关系，比如富客户端在启动过程中要构建缓存，依赖db或者某个rpc，那么这一条链路的bean是都不适合异步的


## 使用说明

在Application启动类模块下加入

```xml

<dependency>
    <groupId>io.github.nineteen</groupId>
    <artifactId>spring-boot-async-init-starter</artifactId>
    <version>仓库还在申请中</version>
</dependency>
```

`application.properties` 中配置：

```properties
# 启用异步初始化，默认false 
spring.async.init.switch=true 
# 启用@PostConstruct 异步初始化，默认false。默认只异步init-method和InitializingBean
spring.async.init.postConstructSwitch=true
# 异步bean的路径，启用异步时必须配置
spring.async.init.basePackage=com.xxxxx,com.xxxx
# 同步初始化白名单，默认为springframework
spring.async.init.syncBeanKeywords=org.springframework
```

spring boot 2.4.0以下版本需要手动设置ApplicationContext，参考：

> com.github.nineteen.async.init.AsyncApplicationContextFactory

## 在单测中使用
```xml

<dependency>
    <groupId>io.github.nineteen</groupId>
    <artifactId>spring-boot-async-init-test</artifactId>
    <version>仓库还在申请中</version>
</dependency>
```

```java
import com.github.nineteen.async.init.test.AsyncInitContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(loader = AsyncInitContextLoader.class)
public class TestApplication {
    
}

```