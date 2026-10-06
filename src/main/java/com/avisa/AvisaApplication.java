package com.avisa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.avisa")
public class AvisaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvisaApplication.class, args);
	}
}
