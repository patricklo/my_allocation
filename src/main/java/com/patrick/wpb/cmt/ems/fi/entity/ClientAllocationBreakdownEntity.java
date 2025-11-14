package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.entity.base.BaseAuditEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_allocation_breakdown")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ClientAllocationBreakdownEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_order_id", nullable = false)
    private TraderOrderEntity order;

    @Column(name = "order_quantity", nullable = false, precision = 20, scale = 4)
    private BigDecimal orderQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false, length = 16)
    @Builder.Default
    private AllocationStatus allocationStatus = AllocationStatus.NEW;

    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;

    @Column(name = "final_allocation", precision = 20, scale = 4)
    private BigDecimal finalAllocation;

    @Column(name = "allocation_percentage", precision = 7, scale = 4)
    private BigDecimal allocationPercentage;

    @Column(name = "estimated_order_size", precision = 20, scale = 4)
    private BigDecimal estimatedOrderSize;

    @Column(name = "yield_limit", precision = 9, scale = 4)
    private BigDecimal yieldLimit;

    @Column(name = "spread_limit", precision = 9, scale = 4)
    private BigDecimal spreadLimit;

    @Column(name = "size_limit", precision = 20, scale = 4)
    private BigDecimal sizeLimit;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;
}

