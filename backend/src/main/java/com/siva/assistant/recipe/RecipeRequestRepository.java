package com.siva.assistant.recipe;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRequestRepository extends JpaRepository<RecipeRequest, Long> {

    /** Every attempt counts towards the daily limit, including failed ones, because they cost money too. */
    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant since);

    Optional<RecipeRequest> findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
            Long userId, String requestHash, RequestStatus status, Instant since);
}
