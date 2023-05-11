package com.ontop.wallet.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    private final ClientProperties clientProperties;
    public RestTemplateConfig(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Bean
    public RestTemplate clientRestTemplate() {
        return new RestTemplateBuilder()
                .rootUri(clientProperties.host())
                .setConnectTimeout(Duration.ofMillis(clientProperties.connectionTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(clientProperties.readTimeoutMs()))
                .build();
    }
}
