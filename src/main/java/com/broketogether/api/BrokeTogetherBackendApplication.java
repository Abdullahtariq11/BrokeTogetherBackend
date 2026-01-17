package com.broketogether.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class BrokeTogetherBackendApplication {

  public static void main(String[] args) {

 // This is the "Bridge" that connects your .env file to Spring's ${} placeholders
    Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    // This line manually pushes the variables into the system so Spring can find them
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    SpringApplication.run(BrokeTogetherBackendApplication.class, args);
  }

}
