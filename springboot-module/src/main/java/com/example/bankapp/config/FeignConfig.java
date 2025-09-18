package com.example.bankapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class FeignConfig {

    @Bean
    public Encoder feignEncoder() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        mapper.registerModule(new JavaTimeModule());
        return new JacksonEncoder(mapper);
    }

    @Bean
    public Decoder feignDecoder() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        mapper.registerModule(new JavaTimeModule());
        return new JacksonDecoder(mapper);
    }
}
