package com.examples.demolog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DemologApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemologApplication.class, args);
    }

}
