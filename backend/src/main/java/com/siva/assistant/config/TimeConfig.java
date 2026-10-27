package com.siva.assistant.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TimeConfig {

    /** Follows the JVM default timezone (set from APP_TIMEZONE); tests replace it with a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
