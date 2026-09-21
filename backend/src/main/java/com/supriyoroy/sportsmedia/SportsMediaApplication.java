package com.supriyoroy.sportsmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsMediaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsMediaApplication.class, args);
    }
}