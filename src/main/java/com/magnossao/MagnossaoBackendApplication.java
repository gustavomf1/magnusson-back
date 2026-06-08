package com.magnossao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MagnossaoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MagnossaoBackendApplication.class, args);
	}

}
