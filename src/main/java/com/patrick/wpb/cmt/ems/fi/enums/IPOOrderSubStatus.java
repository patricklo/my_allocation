package com.patrick.wpb.cmt.ems.fi.enums;

public enum IPOOrderSubStatus {
    NONE("NONE"),
    DONE("DONE"),
    PENDING_REGIONAL_ALLOCATION("Pending Regional Allocation"),
    PENDING_REGIONAL_ALLOCATION_APPROVAL("Pending Regional Allocation Approval"),
    PENDING_CLIENT_ALLOCATION("Pending Client Allocation"),
    PENDING_CLIENT_ALLOCATION_APPROVAL("Pending Client Allocation Approval");

    private final String code;

    IPOOrderSubStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
