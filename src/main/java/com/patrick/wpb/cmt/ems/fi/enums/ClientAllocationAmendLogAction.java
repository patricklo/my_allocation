package com.patrick.wpb.cmt.ems.fi.enums;

public enum ClientAllocationAmendLogAction {
    PENDING_APPROVAL("PENDING_APPROVAL"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String code;

    ClientAllocationAmendLogAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
