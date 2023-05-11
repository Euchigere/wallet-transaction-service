package com.ontop.wallet.config;

import com.ontop.wallet.adapters.messagebroker.KafkaEvent;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
class KafkaConsumerConfig {
    public static final String GROUP_ID = "ontop-group";
    public static final long INTERVAL = 5000;
    public static final long MAX_ATTEMPTS = 1;

    @Value("${ontop.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    @Bean
    public ConsumerFactory<String, KafkaEvent> consumerFactory() {
        final JsonDeserializer<KafkaEvent> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages("com.ontop.wallet.adapters.messagebroker");
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(), deserializer);
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        final BackOff fixedBackOff = new FixedBackOff(INTERVAL, MAX_ATTEMPTS);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
            log.error("Consumer Error", exception);
            // logic to execute when all the retry attemps are exhausted
        }, fixedBackOff);
        errorHandler.addRetryableExceptions(ResourceLockedException.class);
        return errorHandler;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaEvent>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}
