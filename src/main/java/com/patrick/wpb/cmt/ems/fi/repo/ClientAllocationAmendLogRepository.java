package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationAmendLogEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientAllocationAmendLogRepository extends JpaRepository<ClientAllocationAmendLogEntity, Long> {

    Optional<ClientAllocationAmendLogEntity> findFirstByRefIdOrderByRevisionDesc(String refId);

    List<ClientAllocationAmendLogEntity> findByRefIdOrderByRevisionDesc(String refId);
}

