package com.neosow.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NeoSowInfraApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeoSowInfraApplication.class, args);
    }
}
