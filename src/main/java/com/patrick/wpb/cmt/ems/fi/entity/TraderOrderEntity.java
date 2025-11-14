package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.entity.base.BaseAuditEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trader_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TraderOrderEntity extends BaseAuditEntity {

    @Id
    @Column(name = "client_order_id", nullable = false, length = 64)
    private String clientOrderId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IPOOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_status", nullable = false, length = 64)
    private IPOOrderSubStatus subStatus;

    @Column(name = "original_client_order_id", length = 64)
    private String originalClientOrderId;

    @Column(name = "security_id", nullable = false, length = 64)
    private String securityId;

    @Column(name = "order_quantity", nullable = false, precision = 20, scale = 4)
    private BigDecimal orderQuantity;

    @Column(name = "clean_price", precision = 18, scale = 6)
    private BigDecimal cleanPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TraderSubOrderEntity> subOrders = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private RegionalAllocationEntity regionalAllocation;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClientAllocationBreakdownEntity> clientAllocations = new ArrayList<>();
}
