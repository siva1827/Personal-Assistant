package com.siva.assistant.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FcmPushSenderTest {

    private static final String URL = "https://fcm.googleapis.com/v1/projects/my-project/messages:send";
    private static final PushMessage MESSAGE =
            new PushMessage("Due today", "Pay rent", Map.of("link", "/tasks", "type", "TASK_DUE"));

    private MockRestServiceServer server;
    private FcmPushSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fcm.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new FcmPushSender(builder.build(), "my-project", () -> "access-token-123");
    }

    @Test
    void sendsTheMessageToTheProjectWithABearerToken() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token-123"))
                .andExpect(jsonPath("$.message.token").value("device-1"))
                .andExpect(jsonPath("$.message.notification.title").value("Due today"))
                .andExpect(jsonPath("$.message.notification.body").value("Pay rent"))
                .andExpect(jsonPath("$.message.data.link").value("/tasks"))
                .andExpect(jsonPath("$.message.data.type").value("TASK_DUE"))
                .andExpect(jsonPath("$.message.android.priority").value("HIGH"))
                .andRespond(withSuccess("{\"name\":\"projects/my-project/messages/1\"}", MediaType.APPLICATION_JSON));

        assertThat(sender.send("device-1", MESSAGE)).isEqualTo(PushResult.DELIVERED);
        server.verify();
    }

    @Test
    void aMissingDeviceIsReportedAsAnInvalidToken() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"));

        assertThat(sender.send("gone", MESSAGE)).isEqualTo(PushResult.INVALID_TOKEN);
    }

    @Test
    void aMalformedTokenIsReportedAsAnInvalidToken() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"status\":\"INVALID_ARGUMENT\","
                        + "\"message\":\"The registration token is not a valid FCM registration token\"}}"));

        assertThat(sender.send("garbage", MESSAGE)).isEqualTo(PushResult.INVALID_TOKEN);
    }

    @Test
    void aBadMessageDoesNotCountAsADeadToken() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":\"Invalid value at message.data\"}}"));

        assertThat(sender.send("device-1", MESSAGE)).isEqualTo(PushResult.FAILED);
    }

    @Test
    void serverErrorsAndAuthProblemsKeepTheToken() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(sender.send("device-1", MESSAGE)).isEqualTo(PushResult.FAILED);
        assertThat(sender.send("device-1", MESSAGE)).isEqualTo(PushResult.FAILED);
    }

    @Test
    void failingToGetAnAccessTokenIsAFailureNotACrash() {
        FcmPushSender broken = new FcmPushSender(RestClient.builder().baseUrl("https://fcm.googleapis.com").build(),
                "my-project", () -> {
                    throw new IllegalStateException("no token");
                });

        assertThat(broken.send("device-1", MESSAGE)).isEqualTo(PushResult.FAILED);
    }
}
