package com.grjaznovs.jevgenijs.accountapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AccountAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountAppApplication.class, args);
	}

}
