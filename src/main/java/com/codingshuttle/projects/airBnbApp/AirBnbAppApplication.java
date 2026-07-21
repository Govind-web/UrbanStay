package com.codingshuttle.projects.airBnbApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Controller
public class AirBnbAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(AirBnbAppApplication.class, args);
	}

	@GetMapping("/hello")
	public String getHello(){
		return "Govind Thakur Hello";
	}
}
