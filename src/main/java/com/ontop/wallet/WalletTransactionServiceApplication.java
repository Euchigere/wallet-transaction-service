package com.ontop.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class WalletTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletTransactionServiceApplication.class, args);
    }

}
