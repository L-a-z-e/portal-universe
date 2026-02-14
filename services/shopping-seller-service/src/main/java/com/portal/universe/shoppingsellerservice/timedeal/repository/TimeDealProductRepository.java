package com.portal.universe.shoppingsellerservice.timedeal.repository;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeDealProductRepository extends JpaRepository<TimeDealProduct, Long> {
}
