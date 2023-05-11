package com.ontop.wallet.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OntopAccountProperties.class)
public class OntopAccountConfig {}
