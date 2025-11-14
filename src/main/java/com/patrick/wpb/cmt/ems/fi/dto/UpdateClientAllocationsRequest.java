package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class UpdateClientAllocationsRequest {

    @NotEmpty
    @Valid
    private List<ClientAllocationBreakdownRequest> breakdowns;
}

