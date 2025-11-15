package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationBreakdownEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionalAllocationBreakdownRepository extends JpaRepository<RegionalAllocationBreakdownEntity, Long> {
    List<RegionalAllocationBreakdownEntity> findByOrderClientOrderId(String clientOrderId);
}

