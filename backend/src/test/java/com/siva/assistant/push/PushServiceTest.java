package com.siva.assistant.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    private static final long USER = 7L;
    private static final PushMessage MESSAGE = new PushMessage("t", "b", Map.of());

    @Mock
    PushSender sender;
    @Mock
    DeviceTokenRepository devices;

    PushService service;

    @BeforeEach
    void setUp() {
        service = new PushService(sender, devices);
    }

    private static DeviceToken device(String token) {
        return new DeviceToken(USER, token, DevicePlatform.ANDROID, Instant.EPOCH);
    }

    @Test
    void doesNothingWhenFirebaseIsNotConfigured() {
        when(sender.isConfigured()).thenReturn(false);

        assertThat(service.sendToUser(USER, MESSAGE)).isEqualTo(new PushService.Summary(false, 0, 0));
        verify(devices, never()).findByUserId(any());
        verify(sender, never()).send(any(), any());
    }

    @Test
    void sendsToEveryDeviceAndCountsDeliveries() {
        when(sender.isConfigured()).thenReturn(true);
        when(devices.findByUserId(USER)).thenReturn(List.of(device("a"), device("b"), device("c")));
        when(sender.send("a", MESSAGE)).thenReturn(PushResult.DELIVERED);
        when(sender.send("b", MESSAGE)).thenReturn(PushResult.FAILED);
        when(sender.send("c", MESSAGE)).thenReturn(PushResult.DELIVERED);

        assertThat(service.sendToUser(USER, MESSAGE)).isEqualTo(new PushService.Summary(true, 3, 2));
        verify(devices, never()).deleteByToken(any());
    }

    @Test
    void forgetsDevicesThatNoLongerExist() {
        when(sender.isConfigured()).thenReturn(true);
        when(devices.findByUserId(USER)).thenReturn(List.of(device("alive"), device("dead")));
        when(sender.send("alive", MESSAGE)).thenReturn(PushResult.DELIVERED);
        when(sender.send("dead", MESSAGE)).thenReturn(PushResult.INVALID_TOKEN);

        assertThat(service.sendToUser(USER, MESSAGE).delivered()).isEqualTo(1);
        verify(devices).deleteByToken("dead");
        verify(devices, never()).deleteByToken("alive");
    }

    @Test
    void noDevicesMeansNothingIsSent() {
        when(sender.isConfigured()).thenReturn(true);
        when(devices.findByUserId(USER)).thenReturn(List.of());

        assertThat(service.sendToUser(USER, MESSAGE)).isEqualTo(new PushService.Summary(true, 0, 0));
        verify(sender, never()).send(any(), any());
    }
}
