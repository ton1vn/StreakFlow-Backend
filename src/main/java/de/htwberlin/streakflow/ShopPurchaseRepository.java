package de.htwberlin.streakflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, Long> {
    List<ShopPurchase> findByItemId(String itemId);

    Optional<ShopPurchase> findFirstByItemIdAndUsedAtIsNullOrderByPurchasedAtAsc(String itemId);
}
