package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.enums.RegionalAllocationStatus;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionalAllocationBreakdownDto {

    Long id;
    String countryCode;
    String accountNumber;
    BigDecimal orderQuantity;
    BigDecimal allocatedQuantity;
    RegionalAllocationStatus regionalAllocationStatus;
    BigDecimal finalAllocation;
    BigDecimal allocationPercentage;
    BigDecimal estimatedOrderSize;
    BigDecimal yieldLimit;
    BigDecimal spreadLimit;
    BigDecimal sizeLimit;

    public static RegionalAllocationBreakdownDto fromEntity(RegionalAllocationBreakdownEntity entity) {
        return RegionalAllocationBreakdownDto.builder()
                .id(entity.getId())
                .countryCode(entity.getCountryCode())
                .accountNumber(entity.getAccountNumber())
                .orderQuantity(entity.getOrderQuantity())
                .allocatedQuantity(entity.getAllocatedQuantity())
                .regionalAllocationStatus(entity.getRegionalAllocationStatus())
                .finalAllocation(entity.getFinalAllocation())
                .allocationPercentage(entity.getAllocationPercentage())
                .estimatedOrderSize(entity.getEstimatedOrderSize())
                .yieldLimit(entity.getYieldLimit())
                .spreadLimit(entity.getSpreadLimit())
                .sizeLimit(entity.getSizeLimit())
                .build();
    }
}

