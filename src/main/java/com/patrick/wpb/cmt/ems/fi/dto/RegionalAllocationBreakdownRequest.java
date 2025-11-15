package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RegionalAllocationBreakdownRequest {

    @NotBlank
    private String countryCode;

    @NotBlank
    private String accountNumber;

    @NotNull
    private BigDecimal orderQuantity;

    @NotNull
    private BigDecimal finalAllocation;

    private BigDecimal allocationPercentage;

    private BigDecimal estimatedOrderSize;

    private BigDecimal yieldLimit;

    private BigDecimal spreadLimit;

    private BigDecimal sizeLimit;
}

