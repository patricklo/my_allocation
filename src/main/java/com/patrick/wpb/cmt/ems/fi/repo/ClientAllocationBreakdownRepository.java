package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientAllocationBreakdownRepository extends JpaRepository<ClientAllocationBreakdownEntity, Long> {
    List<ClientAllocationBreakdownEntity> findByOrderClientOrderId(String clientOrderId);
}

