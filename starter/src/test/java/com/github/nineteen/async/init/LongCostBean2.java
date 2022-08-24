package com.github.nineteen.async.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class LongCostBean2 {

    private final Logger logger = LoggerFactory.getLogger(LongCostBean2.class);

    @PostConstruct
    public void init2() throws Exception {
        logger.info("{} init2 start", Thread.currentThread().getName());
        TimeUnit.SECONDS.sleep(10L);
        logger.info("{} init2 end", Thread.currentThread().getName());
    }
}
