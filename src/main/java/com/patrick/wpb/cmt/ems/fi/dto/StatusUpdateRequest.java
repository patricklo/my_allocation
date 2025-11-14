package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank
    private String changedBy;

    private String note;
}

