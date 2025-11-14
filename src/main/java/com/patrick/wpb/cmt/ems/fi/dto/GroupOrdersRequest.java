package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class GroupOrdersRequest {

    @NotEmpty
    private List<String> clientOrderIds;

    @NotNull
    private String createdBy;
}

