package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patrick.wpb.cmt.ems.fi.entity.FinalPricedAllocationBreakdownEntity;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalPricedAllocationBreakdownDto {

    Long id;
    String countryCode;
    String limitType;
    BigDecimal finalPrice;

    public static FinalPricedAllocationBreakdownDto fromEntity(FinalPricedAllocationBreakdownEntity entity) {
        return FinalPricedAllocationBreakdownDto.builder()
                .id(entity.getId())
                .countryCode(entity.getCountryCode())
                .limitType(entity.getLimitType())
                .finalPrice(entity.getFinalPrice())
                .build();
    }
}

