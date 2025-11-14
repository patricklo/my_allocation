package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.entity.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "final_regional_allocation", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_order_id", "market"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FinalRegionalAllocationEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_order_id", nullable = false, length = 64)
    private String clientOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_order_id", insertable = false, updatable = false)
    private TraderOrderEntity order;

    @Column(name = "asia_allocation", precision = 20, scale = 4)
    private BigDecimal asiaAllocation;

    @Column(name = "allocation", precision = 20, scale = 4)
    private BigDecimal allocation;

    @Column(name = "market", nullable = false, length = 32)
    private String market;

    @Column(name = "effective_order", precision = 20, scale = 4)
    private BigDecimal effectiveOrder;

    @Column(name = "pro_rata", precision = 7, scale = 4)
    private BigDecimal proRata;

    @Column(name = "allocation_amount", precision = 20, scale = 4)
    private BigDecimal allocationAmount;
}

