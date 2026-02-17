package com.kafka.synthetic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyntheticClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyntheticClientApplication.class, args);
    }
}
