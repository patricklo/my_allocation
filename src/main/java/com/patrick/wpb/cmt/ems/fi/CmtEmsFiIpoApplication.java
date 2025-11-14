package com.patrick.wpb.cmt.ems.fi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CmtEmsFiIpoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmtEmsFiIpoApplication.class, args);
    }
}

