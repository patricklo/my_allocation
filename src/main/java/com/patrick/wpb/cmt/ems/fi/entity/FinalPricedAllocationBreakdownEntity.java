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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "final_priced_allocation_breakdown")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FinalPricedAllocationBreakdownEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_order_id", nullable = false)
    private TraderOrderEntity order;

    @Column(name = "limit_type", length = 32)
    private String limitType;

    @Column(name = "final_price", precision = 18, scale = 6)
    private BigDecimal finalPrice;

    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;
}

