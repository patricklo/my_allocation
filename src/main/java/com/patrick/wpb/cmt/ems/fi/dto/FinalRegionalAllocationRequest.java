package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinalRegionalAllocationRequest {

    @NotNull
    private BigDecimal asiaAllocation;

    @NotNull
    private BigDecimal allocation;

    private String market;

    @NotNull
    private BigDecimal effectiveOrder;

    @NotNull
    private BigDecimal proRata;

    @NotNull
    private BigDecimal allocationAmount;
}

