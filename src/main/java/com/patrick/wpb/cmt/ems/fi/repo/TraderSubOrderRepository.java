package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.TraderSubOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraderSubOrderRepository extends JpaRepository<TraderSubOrderEntity, Long> {
    long countByOrderClientOrderIdAndIssueIPOFlagTrue(String clientOrderId);
    List<TraderSubOrderEntity> findByOrderClientOrderId(String clientOrderId);
}
