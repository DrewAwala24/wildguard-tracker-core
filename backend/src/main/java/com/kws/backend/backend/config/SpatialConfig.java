package com.kws.backend.backend.config;

import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpatialConfig {

    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}