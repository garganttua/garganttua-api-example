package com.garganttua.api.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.garganttua"})
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class GarganttuaAPIExampleBackend {
	
	public static void main(String[] args) {
		SpringApplication.run(GarganttuaAPIExampleBackend.class, args);
	}

}
