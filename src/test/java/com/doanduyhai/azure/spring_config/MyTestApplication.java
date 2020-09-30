package com.doanduyhai.azure.spring_config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MyTestApplication {

    private static final Logger log = LoggerFactory.getLogger(MyTestApplication.class);

    public static void main(String... args) {

        log.info("************** Starting application *******************");
        ConfigurableApplicationContext context = SpringApplication.run(MyTestApplication.class, args);

//        ConfigurableEnvironment environment = context.getEnvironment();
//        environment.getPropertySources()
//                .stream()
//                .forEach(propertySource -> log.info("Property source : " + propertySource.getName()));
    }
}
