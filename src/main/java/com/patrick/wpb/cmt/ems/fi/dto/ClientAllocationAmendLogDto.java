package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationAmendLogEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentAction;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentObjectType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAllocationAmendLogDto {

    Long id;
    Integer revision;
    String refId;
    AmendmentObjectType objectType;
    String beforeObjectJson;
    String afterObjectJson;
    AmendmentAction action;
    String createdBy;

    public static ClientAllocationAmendLogDto fromEntity(ClientAllocationAmendLogEntity entity) {
        return ClientAllocationAmendLogDto.builder()
                .id(entity.getId())
                .revision(entity.getRevision())
                .refId(entity.getRefId())
                .objectType(entity.getObjectType())
                .beforeObjectJson(entity.getBeforeObjectJson())
                .afterObjectJson(entity.getAfterObjectJson())
                .action(entity.getAction())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}

