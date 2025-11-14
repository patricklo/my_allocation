package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderStatusAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraderOrderStatusAuditRepository extends JpaRepository<TraderOrderStatusAuditEntity, Long> {
}

