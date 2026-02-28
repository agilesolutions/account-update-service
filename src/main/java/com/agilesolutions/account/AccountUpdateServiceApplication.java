// AccountUpdateServiceApplication.java
package com.agilesolutions.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AccountUpdateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountUpdateServiceApplication.class, args);
    }
}