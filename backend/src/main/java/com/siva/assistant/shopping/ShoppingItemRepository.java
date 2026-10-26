package com.siva.assistant.shopping;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    /** Items still to buy first, then bought ones. */
    List<ShoppingItem> findByUserIdOrderByPurchasedAscIdAsc(Long userId);

    Optional<ShoppingItem> findByIdAndUserId(Long id, Long userId);

    List<ShoppingItem> findByUserIdAndPurchasedFalse(Long userId);

    long deleteByUserIdAndPurchasedTrue(Long userId);
}
