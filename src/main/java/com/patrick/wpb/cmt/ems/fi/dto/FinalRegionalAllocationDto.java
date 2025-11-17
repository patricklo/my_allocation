package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patrick.wpb.cmt.ems.fi.entity.FinalRegionalAllocationEntity;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalRegionalAllocationDto {

    Long id;
    String market;
    BigDecimal asiaAllocation;
    BigDecimal allocation;
    BigDecimal effectiveOrder;
    BigDecimal proRata;
    BigDecimal allocationAmount;

    public static FinalRegionalAllocationDto fromEntity(FinalRegionalAllocationEntity entity) {
        return FinalRegionalAllocationDto.builder()
                .id(entity.getId())
                .market(entity.getMarket())
                .asiaAllocation(entity.getAsiaAllocation())
                .allocation(entity.getAllocation())
                .effectiveOrder(entity.getEffectiveOrder())
                .proRata(entity.getProRata())
                .allocationAmount(entity.getAllocationAmount())
                .build();
    }
}

