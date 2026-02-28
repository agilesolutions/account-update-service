package com.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CardUpdateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardUpdateServiceApplication.class, args);
    }
}