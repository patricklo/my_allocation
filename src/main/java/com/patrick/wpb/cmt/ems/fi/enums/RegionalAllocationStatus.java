package com.patrick.wpb.cmt.ems.fi.enums;

public enum RegionalAllocationStatus {
    NEW("NEW"),
    ACCEPTED("ACCEPTED");

    private final String code;

    RegionalAllocationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
