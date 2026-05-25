package com.sealhackathon.api;

import com.sealhackathon.api.config.DotenvEnvironment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeApplication {

    public static void main(String[] args) {
        DotenvEnvironment.load();
        SpringApplication.run(BeApplication.class, args);
    }

}
