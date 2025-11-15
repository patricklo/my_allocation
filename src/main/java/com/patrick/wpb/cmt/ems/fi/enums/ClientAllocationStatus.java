package com.patrick.wpb.cmt.ems.fi.enums;

public enum ClientAllocationStatus {
    NEW("NEW"),
    ACCEPTED("ACCEPTED");

    private final String code;

    ClientAllocationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
