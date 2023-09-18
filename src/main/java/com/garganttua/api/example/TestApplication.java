package com.garganttua.api.example;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;

import com.garganttua.api.security.IGGAPISecurityHelper;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@EnableWebSecurity
@ComponentScan({"com.garganttua"})
@Configuration
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class TestApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}
	

	@Autowired
	private Optional<IGGAPISecurityHelper> securityHelper;

	@Bean
	public DefaultSecurityFilterChain configureFilterChainTest(HttpSecurity http) throws Exception {
		http.csrf().disable()
		.authorizeHttpRequests()
        .requestMatchers("/swagger-ui.html","/swagger-ui/**","/v3/**").permitAll().and()
		.authorizeHttpRequests()
		;
		
		this.securityHelper.ifPresent(securityHelper ->{
				securityHelper.configureFilterChain(http);
		});
		http.cors();
		return http.build();
	}
	
	@Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Test API").description(
                        "This is a sample Spring Boot RESTful service using springdoc-openapi and OpenAPI 3."));
    }

}
