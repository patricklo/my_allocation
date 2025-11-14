package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trader_order_status_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraderOrderStatusAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_order_id", nullable = false)
    private TraderOrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private IPOOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_sub_status", length = 64)
    private IPOOrderSubStatus fromSubStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private IPOOrderStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_sub_status", nullable = false, length = 64)
    private IPOOrderSubStatus toSubStatus;

    @Column(name = "changed_by", nullable = false, length = 64)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "note", length = 256)
    private String note;
}

