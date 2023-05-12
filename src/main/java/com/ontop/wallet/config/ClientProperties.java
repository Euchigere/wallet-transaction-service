package com.ontop.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("client")
record ClientProperties(String host, int connectionTimeoutMs, int readTimeoutMs) { }
