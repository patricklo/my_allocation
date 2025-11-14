package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AllocationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAllocationBreakdownDto {

    Long id;

    @NotBlank
    String countryCode;

    @NotBlank
    String accountNumber;

    @NotNull
    BigDecimal orderQuantity;

    AllocationStatus allocationStatus;

    BigDecimal finalAllocation;

    BigDecimal allocationPercentage;

    BigDecimal estimatedOrderSize;

    BigDecimal yieldLimit;

    BigDecimal spreadLimit;

    BigDecimal sizeLimit;

    public static ClientAllocationBreakdownDto fromEntity(ClientAllocationBreakdownEntity entity) {
        return ClientAllocationBreakdownDto.builder()
                .id(entity.getId())
                .countryCode(entity.getCountryCode())
                .accountNumber(entity.getAccountNumber())
                .orderQuantity(entity.getOrderQuantity())
                .allocationStatus(entity.getAllocationStatus())
                .finalAllocation(entity.getFinalAllocation())
                .allocationPercentage(entity.getAllocationPercentage())
                .estimatedOrderSize(entity.getEstimatedOrderSize())
                .yieldLimit(entity.getYieldLimit())
                .spreadLimit(entity.getSpreadLimit())
                .sizeLimit(entity.getSizeLimit())
                .build();
    }
}

