package com.example.coffeeordersystem.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("!test & !concurrency")
@EnableScheduling
public class OutboxSchedulingConfig {}
