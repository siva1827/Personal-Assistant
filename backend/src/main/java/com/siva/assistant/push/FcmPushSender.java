package com.siva.assistant.push;

import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Sends messages through the Firebase Cloud Messaging HTTP v1 API.
 * https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages/send
 */
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final RestClient http;
    private final String projectId;
    private final Supplier<String> accessToken;

    /**
     * @param http        a client whose base URL is https://fcm.googleapis.com
     * @param accessToken supplies a current OAuth2 token for the service account
     */
    public FcmPushSender(RestClient http, String projectId, Supplier<String> accessToken) {
        this.http = http;
        this.projectId = projectId;
        this.accessToken = accessToken;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        Map<String, Object> body = Map.of("message", Map.of(
                "token", deviceToken,
                "notification", Map.of("title", message.title(), "body", message.body()),
                "data", message.data(),
                "android", Map.of("priority", "HIGH")));
        try {
            http.post()
                    .uri("/v1/projects/{project}/messages:send", projectId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.get())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return PushResult.DELIVERED;
        } catch (HttpStatusCodeException e) {
            if (tokenIsGone(e)) {
                return PushResult.INVALID_TOKEN;
            }
            log.warn("FCM rejected a message ({}): {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return PushResult.FAILED;
        } catch (RuntimeException e) {
            // One short line; the usual causes are bad credentials or no network, not a bug to trace
            log.warn("Could not send via FCM: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
            log.debug("FCM send failure details", e);
            return PushResult.FAILED;
        }
    }

    /**
     * FCM reports a dead token as 404 / UNREGISTERED, or as 400 INVALID_ARGUMENT mentioning the
     * registration token. Any other 400 means the message itself is wrong and must not delete tokens.
     */
    static boolean tokenIsGone(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        if (e.getStatusCode().value() == 404 || body.contains("UNREGISTERED")) {
            return true;
        }
        return e.getStatusCode().value() == 400
                && body.contains("INVALID_ARGUMENT")
                && body.toLowerCase().contains("registration token");
    }
}
