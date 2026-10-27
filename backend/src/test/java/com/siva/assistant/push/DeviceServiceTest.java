package com.siva.assistant.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T05:00:00Z");

    @Mock
    DeviceTokenRepository devices;

    DeviceService service;

    @BeforeEach
    void setUp() {
        service = new DeviceService(devices, Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    @Test
    void registersANewDevice() {
        when(devices.findByToken("tok")).thenReturn(Optional.empty());
        when(devices.findByUserIdOrderByLastSeenAtAscIdAsc(7L)).thenReturn(List.of());

        service.register(7L, "tok", DevicePlatform.ANDROID);

        ArgumentCaptor<DeviceToken> saved = ArgumentCaptor.forClass(DeviceToken.class);
        verify(devices).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getToken()).isEqualTo("tok");
        assertThat(saved.getValue().getLastSeenAt()).isEqualTo(NOW);
    }

    @Test
    void registeringAgainRefreshesInsteadOfDuplicating() {
        DeviceToken existing = new DeviceToken(7L, "tok", DevicePlatform.ANDROID, Instant.EPOCH);
        when(devices.findByToken("tok")).thenReturn(Optional.of(existing));
        when(devices.findByUserIdOrderByLastSeenAtAscIdAsc(7L)).thenReturn(List.of(existing));

        service.register(7L, "tok", DevicePlatform.ANDROID);

        verify(devices, never()).save(any());
        assertThat(existing.getLastSeenAt()).isEqualTo(NOW);
    }

    @Test
    void aDeviceMovesToWhoeverSignedInLast() {
        DeviceToken existing = new DeviceToken(7L, "tok", DevicePlatform.ANDROID, Instant.EPOCH);
        when(devices.findByToken("tok")).thenReturn(Optional.of(existing));
        when(devices.findByUserIdOrderByLastSeenAtAscIdAsc(8L)).thenReturn(List.of(existing));

        service.register(8L, "tok", DevicePlatform.ANDROID);

        assertThat(existing.getUserId()).isEqualTo(8L);
    }

    @Test
    void dropsTheLeastRecentlyUsedDevicesBeyondTheLimit() {
        List<DeviceToken> all = new ArrayList<>();
        for (int i = 0; i < DeviceService.MAX_DEVICES_PER_USER + 2; i++) {
            all.add(new DeviceToken(7L, "t" + i, DevicePlatform.ANDROID, Instant.EPOCH.plusSeconds(i)));
        }
        when(devices.findByToken("t11")).thenReturn(Optional.of(all.get(11)));
        when(devices.findByUserIdOrderByLastSeenAtAscIdAsc(7L)).thenReturn(all);

        service.register(7L, "t11", DevicePlatform.ANDROID);

        ArgumentCaptor<DeviceToken> removed = ArgumentCaptor.forClass(DeviceToken.class);
        verify(devices, times(2)).delete(removed.capture());
        assertThat(removed.getAllValues()).extracting(DeviceToken::getToken).containsExactly("t0", "t1");
    }

    @Test
    void unregisterOnlyRemovesTheCallersOwnDevice() {
        service.unregister(7L, "tok");

        verify(devices).deleteByTokenAndUserId("tok", 7L);
    }
}
