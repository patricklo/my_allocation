package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionalAllocationBreakdownSummaryDto {

    String countryCode;
    BigDecimal totalOrderQuantity;
    BigDecimal totalAllocatedQuantity;
}

