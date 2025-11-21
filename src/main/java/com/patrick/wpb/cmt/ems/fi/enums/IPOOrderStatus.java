package com.patrick.wpb.cmt.ems.fi.enums;

public enum IPOOrderStatus {
    NEW("NEW ORDER"),
    ACCEPTED("Accepted Order"),
    PLACED("Order Placed"),
    REGIONAL_ALLOCATION("Regional Allocation"),
    CLIENT_ALLOCATION("Client Allocation"),
    CANCELLED("Cancelled Order");

    private final String code;

    IPOOrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
