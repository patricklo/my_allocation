package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SubmitClientAllocationRequest {

    @NotEmpty
    @Valid
    private List<ClientAllocationBreakdownRequest> breakdowns;

    @NotBlank
    private String changedBy;

    private String note;
}

