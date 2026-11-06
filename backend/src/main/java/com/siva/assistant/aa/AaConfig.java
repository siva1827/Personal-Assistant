package com.siva.assistant.aa;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AaConfig {

    private static final Logger log = LoggerFactory.getLogger(AaConfig.class);

    @Bean
    AaProvider aaProvider(AaProperties properties, Clock clock) {
        switch (properties.provider()) {
            case "none":
                log.info("Bank connect is off (set AA_PROVIDER=mock to try the demo)");
                return new NoAaProvider();
            case "mock":
                log.warn("Bank connect is running in DEMO mode: accounts and transactions are invented");
                return new MockAaProvider(clock);
            default:
                throw new IllegalStateException("Unknown AA_PROVIDER '" + properties.provider()
                        + "'. Use none or mock; a real provider needs its own adapter (see docs/BANK_CONNECT.md)");
        }
    }
}
