package com.siva.assistant;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AssistantApplication {

    public static void main(String[] args) {
        // "Today" (due dates, budgets, reminders) follows the user's timezone, not the server's.
        // Must be set before Spring starts.
        String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(AssistantApplication.class, args);
    }
}
