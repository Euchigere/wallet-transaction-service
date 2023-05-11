package com.ontop.wallet;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration(proxyBeanMethods = false)
public class Mocks {
    @Bean
    @Primary
    KafkaAdmin kafkaAdmin() {
        return Mockito.mock(KafkaAdmin.class);
    }

    @Bean
    @Primary
    KafkaTemplate kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
