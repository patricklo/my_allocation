package com.patrick.wpb.cmt.ems.fi.enums;

public enum AmendmentAction {
    PENDING_APPROVAL("PENDING_APPROVAL"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String code;

    AmendmentAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

