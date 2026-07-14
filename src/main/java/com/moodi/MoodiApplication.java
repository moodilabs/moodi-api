package com.moodi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MoodiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoodiApplication.class, args);
	}

}
