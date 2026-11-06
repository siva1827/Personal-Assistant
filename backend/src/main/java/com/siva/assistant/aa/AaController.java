package com.siva.assistant.aa;

import com.siva.assistant.aa.AaDtos.ConsentResponse;
import com.siva.assistant.aa.AaDtos.CreateConsentRequest;
import com.siva.assistant.aa.AaDtos.StatusResponse;
import com.siva.assistant.aa.AaDtos.SyncResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aa")
public class AaController {

    private final AaService aaService;

    public AaController(AaService aaService) {
        this.aaService = aaService;
    }

    /** Whether bank connect is set up, and whether it is the demo provider. Works even when it is off. */
    @GetMapping("/status")
    public StatusResponse status() {
        return aaService.status();
    }

    @GetMapping("/consents")
    public List<ConsentResponse> list(@AuthenticationPrincipal Long userId) {
        return aaService.list(userId);
    }

    @PostMapping("/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponse create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody(required = false) CreateConsentRequest request) {
        return aaService.create(userId, request == null ? null : request.historyDays());
    }

    /** Demo provider only: acts as the user approving in their Account Aggregator app. */
    @PostMapping("/consents/{id}/mock-approve")
    public ConsentResponse mockApprove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return aaService.decide(userId, id, true);
    }

    /** Demo provider only: acts as the user rejecting in their Account Aggregator app. */
    @PostMapping("/consents/{id}/mock-reject")
    public ConsentResponse mockReject(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return aaService.decide(userId, id, false);
    }

    @PostMapping("/consents/{id}/sync")
    public SyncResult sync(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return aaService.sync(userId, id);
    }

    @DeleteMapping("/consents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        aaService.revoke(userId, id);
    }
}
