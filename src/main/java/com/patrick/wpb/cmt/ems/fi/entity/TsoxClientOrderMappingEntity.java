package com.patrick.wpb.cmt.ems.fi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

import java.sql.Timestamp;

public class TsoxClientOrderMappingEntity {

    @Id
    @Column(name = "tsox_client_order_id")
    private String tsoxClientOrderId;

    @Column(name = "active_flag")
    private String activeFlag;

    @Column(name = "previous_tsox_client_order_id")
    private String previousTsoxClientOrderId;

    @Column(name = "booking_center")
    private String bookingCenter;

    @Column(name = "client_order_id")
    private String clientOrderId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "update_by")
    private String updateBy;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "update_at")
    private Timestamp updateAt;
}
