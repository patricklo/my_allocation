package com.patrick.wpb.cmt.ems.fi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinalPricedAllocationBreakdownRequest {

    @NotBlank
    private String countryCode;

    private String limitType;

    @NotNull
    private BigDecimal finalPrice;
}

