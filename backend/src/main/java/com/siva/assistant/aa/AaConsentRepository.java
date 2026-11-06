package com.siva.assistant.aa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AaConsentRepository extends JpaRepository<AaConsent, Long> {

    List<AaConsent> findByUserIdOrderByIdDesc(Long userId);

    Optional<AaConsent> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndStatusIn(Long userId, Collection<ConsentStatus> statuses);
}
