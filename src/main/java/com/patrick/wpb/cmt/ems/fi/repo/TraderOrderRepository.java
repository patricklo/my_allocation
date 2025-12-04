package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraderOrderRepository extends JpaRepository<TraderOrderEntity, String> {

    List<TraderOrderEntity> findByStatusAndSubStatus(IPOOrderStatus status, IPOOrderSubStatus subStatus);

    List<TraderOrderEntity> findByStatus(IPOOrderStatus status);

    List<TraderOrderEntity> findByTradeDateAndSecurityIdAndStatusAndSubStatusAndRegionalAllocationIsNull(
            LocalDate tradeDate,
            String securityId,
            IPOOrderStatus status,
            IPOOrderSubStatus subStatus);

    List<TraderOrderEntity> findByOriginalClientOrderId(String originalClientOrderId);
}
