package com.siva.assistant.push;

import com.siva.assistant.push.DeviceDtos.RegisterRequest;
import com.siva.assistant.push.DeviceDtos.UnregisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final PushService pushService;

    public DeviceController(DeviceService deviceService, PushService pushService) {
        this.deviceService = deviceService;
        this.pushService = pushService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Long userId, @Valid @RequestBody RegisterRequest request) {
        deviceService.register(userId, request.token(), request.platform());
    }

    @PostMapping("/unregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal Long userId, @Valid @RequestBody UnregisterRequest request) {
        deviceService.unregister(userId, request.token());
    }

    /** Sends a test message to this user's devices, so setup can be checked in one tap. */
    @PostMapping("/test")
    public PushService.Summary test(@AuthenticationPrincipal Long userId) {
        return pushService.sendToUser(userId, new PushMessage(
                "Test notification", "Push notifications are working.", Map.of("link", "/notifications", "type", "TEST")));
    }
}
