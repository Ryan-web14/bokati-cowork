package com.sni.bokaticowork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BokatiCoworkApplication {

	public static void main(String[] args) {
		SpringApplication.run(BokatiCoworkApplication.class, args);
	}

}
