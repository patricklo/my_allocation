package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.FinalRegionalAllocationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinalRegionalAllocationRepository extends JpaRepository<FinalRegionalAllocationEntity, Long> {
    List<FinalRegionalAllocationEntity> findByClientOrderId(String clientOrderId);
    void deleteByClientOrderId(String clientOrderId);
}

