package com.teamvoy.shop.repository;

import com.teamvoy.shop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "SELECT * FROM orders WHERE user_id = :userId", nativeQuery = true)
    List<Order> getAllByUser(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM orders WHERE id = :orderId AND user_id = :userId", nativeQuery = true)
    Optional<Order> getByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
