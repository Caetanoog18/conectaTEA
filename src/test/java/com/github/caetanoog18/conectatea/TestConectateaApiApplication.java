package com.github.caetanoog18.conectatea;

import org.springframework.boot.SpringApplication;

public class TestConectateaApiApplication {
	public static void main(String[] args) {
		SpringApplication.from(ConectateaApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
