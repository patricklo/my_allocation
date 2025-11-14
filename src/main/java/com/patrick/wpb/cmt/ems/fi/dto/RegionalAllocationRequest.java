package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RegionalAllocationRequest {

    @NotNull
    private BigDecimal hkOrderQuantity;

    @NotNull
    private BigDecimal sgOrderQuantity;

    private BigDecimal limitValue;

    private String limitType;

    private BigDecimal sizeLimit;
}

