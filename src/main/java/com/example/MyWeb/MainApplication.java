
package com.example.MyWeb;

import com.example.MyWeb.model.Role;
import com.example.MyWeb.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableAsync
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

	@Bean
	public CommandLineRunner initRoles(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.findByCode("CUSTOMER").isEmpty()) {
				Role role = Role.builder()
						.code("CUSTOMER")
						.name("Customer")
						.description("Customer role")
						.build();
				roleRepository.save(role);
			}
		};
	}
}
