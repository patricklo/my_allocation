package com.patrick.wpb.cmt.ems.fi.enums;

public enum AllocationStatus {
    NEW("NEW"),
    ACCEPTED("ACCEPTED");

    private final String code;

    AllocationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
