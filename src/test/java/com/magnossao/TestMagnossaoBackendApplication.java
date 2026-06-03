package com.magnossao;

import org.springframework.boot.SpringApplication;

public class TestMagnossaoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MagnossaoBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
