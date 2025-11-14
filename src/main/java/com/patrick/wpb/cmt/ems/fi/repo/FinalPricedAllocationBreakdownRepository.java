package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.FinalPricedAllocationBreakdownEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinalPricedAllocationBreakdownRepository extends JpaRepository<FinalPricedAllocationBreakdownEntity, Long> {
    List<FinalPricedAllocationBreakdownEntity> findByOrderClientOrderId(String clientOrderId);
    void deleteByOrderClientOrderId(String clientOrderId);
}

