package com.github.nineteen.async.init;

import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public class AsyncApplicationContextFactory implements ApplicationContextFactory {
    @Override
    public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
        switch (webApplicationType) {
            case SERVLET:{
                return new WebServletAsyncApplicationContext();
            }
            case REACTIVE:
                return new WebReactiveAsyncApplicationContext();

        }
        return new AsyncApplicationContext();
    }
}
