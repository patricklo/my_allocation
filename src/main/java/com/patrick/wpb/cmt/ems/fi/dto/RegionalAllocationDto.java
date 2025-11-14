package com.patrick.wpb.cmt.ems.fi.dto;

import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationEntity;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegionalAllocationDto {
    String clientOrderId;
    BigDecimal orderQuantity;
    BigDecimal hkOrderQuantity;
    BigDecimal sgOrderQuantity;
    BigDecimal limitValue;
    String limitType;
    BigDecimal sizeLimit;

    public static RegionalAllocationDto fromEntity(RegionalAllocationEntity entity) {
        return RegionalAllocationDto.builder()
                .clientOrderId(entity.getClientOrderId())
                .orderQuantity(entity.getOrderQuantity())
                .hkOrderQuantity(entity.getHkOrderQuantity())
                .sgOrderQuantity(entity.getSgOrderQuantity())
                .limitValue(entity.getLimitValue())
                .limitType(entity.getLimitType())
                .sizeLimit(entity.getSizeLimit())
                .build();
    }
}

