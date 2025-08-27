package com.xkrexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for XKR Exchange
 * 
 * This replaces the old Main.java and provides:
 * - Automatic configuration of Spring components
 * - Embedded web server (Tomcat)
 * - Component scanning for all @Service, @Controller, @Repository classes
 * - Auto-configuration of database, security, and Kafka
 */
@SpringBootApplication
public class XkrExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XkrExchangeApplication.class, args);
        System.out.println("🚀 XKR Exchange started successfully!");
        System.out.println("📊 Order Gateway Service: http://localhost:8080/api/v1/orders");
        System.out.println("🏥 Health Check: http://localhost:8080/api/actuator/health");
    }
} 