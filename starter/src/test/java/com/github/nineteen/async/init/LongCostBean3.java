package com.github.nineteen.async.init;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LongCostBean3 implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        TimeUnit.SECONDS.sleep(10L);
    }
}
