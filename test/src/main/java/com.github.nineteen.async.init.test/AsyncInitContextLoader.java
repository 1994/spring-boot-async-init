package com.github.nineteen.async.init.test;

import com.github.nineteen.async.init.AsyncApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootContextLoader;

public class AsyncInitContextLoader extends SpringBootContextLoader {
    @Override
    protected SpringApplication getSpringApplication() {
        SpringApplication springApplication = super.getSpringApplication();
        springApplication.setApplicationContextFactory(new AsyncApplicationContextFactory());
        return springApplication;
    }
}
