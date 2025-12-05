package com.patrick.wpb.cmt.ems.fi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_execution_detail")
public class OrderExecutionDetailEntity {
    @Id
    @Column(name = "execution_id")
    private String executionId;

    @Column(name= "client_order_id")
    private String clientOrderId;

    @Column(name = "booking_center")
    private String bookingCenter;

    @NotNull
    @Column(name = "place_method")
    private String placeMethod;

    @Column(name="broker_code")
    private String brokerCode;

    @Column(name = "counterparty_code")
    private String counterpartyCode;

    @Column(name = "side")
    private char side;

    @Column(name="security_id")
    private String securityId;

    @Column(name="currency")
    private String currency;

    @Column(name ="executed_size")
    private BigDecimal executedSize;

    @Column(name = "executed_price")
    private BigDecimal executedPrice;


}
