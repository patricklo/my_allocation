package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.OrderExecutionDetailEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderExecutionDetailRepository extends JpaRepository<OrderExecutionDetailEntity, String> {

    /**
     * Finds execution detail by client order ID.
     * 
     * @param clientOrderId The client order ID
     * @return Optional containing the execution detail if found
     */
    Optional<OrderExecutionDetailEntity> findByClientOrderId(String clientOrderId);

    /**
     * Finds all execution details by client order ID.
     * 
     * @param clientOrderId The client order ID
     * @return List of execution details
     */
    List<OrderExecutionDetailEntity> findAllByClientOrderId(String clientOrderId);
}

