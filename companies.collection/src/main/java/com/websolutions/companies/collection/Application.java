package com.websolutions.companies.collection;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.websolutions.companies.collection.services.ExpleoJobCollector;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private final ExpleoJobCollector expleoJobCollector;

    public Application(ExpleoJobCollector expleoJobCollector) {
        this.expleoJobCollector = expleoJobCollector;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        expleoJobCollector.scrapeJobs();
    }
}
