package com.github.nineteen.async.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LongCostBean implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(LongCostBean.class);
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("{} init start", Thread.currentThread().getName());
        TimeUnit.SECONDS.sleep(10L);
        logger.info("{} init end", Thread.currentThread().getName());
    }
}
