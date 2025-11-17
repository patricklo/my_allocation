package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.entity.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regional_allocation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RegionalAllocationEntity extends BaseAuditEntity {

    @Id
    @Column(name = "client_order_id", length = 64)
    private String clientOrderId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "client_order_id", referencedColumnName = "client_order_id")
    private TraderOrderEntity order;

    @Column(name = "order_quantity", nullable = false, precision = 20, scale = 4)
    private BigDecimal orderQuantity;

    @Column(name = "hk_order_quantity", nullable = false, precision = 20, scale = 4)
    private BigDecimal hkOrderQuantity;

    @Column(name = "sg_order_quantity", nullable = false, precision = 20, scale = 4)
    private BigDecimal sgOrderQuantity;

    @Column(name = "limit_value", precision = 20, scale = 4)
    private BigDecimal limitValue;

    @Column(name = "limit_type", length = 32)
    private String limitType;

    @Column(name = "size_limit", precision = 20, scale = 4)
    private BigDecimal sizeLimit;
}

