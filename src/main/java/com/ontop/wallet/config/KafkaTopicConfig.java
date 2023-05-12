package com.ontop.wallet.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String TRANSFER_INITIALISED = "transfer-initialised";
    public static final String TRANSFER_PROCESSING_FAILED = "transfer-processing-failed";

    @Bean
    public NewTopic transferProcessing() {
        return TopicBuilder.name(TRANSFER_INITIALISED).build();
    }

    @Bean
    public NewTopic transferRevert() {
        return TopicBuilder.name(TRANSFER_PROCESSING_FAILED).build();
    }
}
