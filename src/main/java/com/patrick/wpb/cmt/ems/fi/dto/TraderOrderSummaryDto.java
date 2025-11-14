package com.patrick.wpb.cmt.ems.fi.dto;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TraderOrderSummaryDto {
    String clientOrderId;
    LocalDate tradeDate;
    String countryCode;
    IPOOrderStatus status;
    IPOOrderSubStatus subStatus;
    String originalClientOrderId;
    String securityId;
    BigDecimal orderQuantity;
    BigDecimal cleanPrice;

    public static TraderOrderSummaryDto fromEntity(TraderOrderEntity entity) {
        return TraderOrderSummaryDto.builder()
                .clientOrderId(entity.getClientOrderId())
                .tradeDate(entity.getTradeDate())
                .countryCode(entity.getCountryCode())
                .status(entity.getStatus())
                .subStatus(entity.getSubStatus())
                .originalClientOrderId(entity.getOriginalClientOrderId())
                .securityId(entity.getSecurityId())
                .orderQuantity(entity.getOrderQuantity())
                .cleanPrice(entity.getCleanPrice())
                .build();
    }
}

