package com.siva.assistant.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(Long userId);

    /** Oldest first, so the least recently used can be dropped when a user has too many. */
    List<DeviceToken> findByUserIdOrderByLastSeenAtAscIdAsc(Long userId);

    long deleteByTokenAndUserId(String token, Long userId);

    long deleteByToken(String token);
}
