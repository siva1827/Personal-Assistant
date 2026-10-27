package com.siva.assistant.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    /**
     * Real push when a Firebase service-account key is provided, otherwise a harmless no-op.
     * A key that is set but cannot be read stops startup, so a typo is noticed immediately.
     */
    @Bean
    PushSender pushSender(@Value("${app.firebase.credentials-file:}") String credentialsFile,
                          @Value("${app.firebase.credentials-json:}") String credentialsJson,
                          ObjectMapper objectMapper) {
        byte[] key;
        try {
            if (!credentialsJson.isBlank()) {
                key = credentialsJson.getBytes(StandardCharsets.UTF_8);
            } else if (!credentialsFile.isBlank()) {
                key = Files.readAllBytes(Path.of(credentialsFile));
            } else {
                log.info("Push notifications are off: no Firebase credentials configured");
                return new NoopPushSender();
            }
            String projectId = objectMapper.readTree(key).path("project_id").asText();
            if (projectId.isBlank()) {
                throw new IllegalStateException("The Firebase credentials have no project_id; use a service-account key");
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(key))
                    .createScoped(List.of(FCM_SCOPE));
            log.info("Push notifications are on for Firebase project {}", projectId);
            RestClient http = RestClient.builder().baseUrl("https://fcm.googleapis.com").build();
            return new FcmPushSender(http, projectId, () -> accessToken(credentials));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the Firebase credentials", e);
        }
    }

    private static synchronized String accessToken(GoogleCredentials credentials) {
        try {
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException("Could not get a Firebase access token", e);
        }
    }
}
