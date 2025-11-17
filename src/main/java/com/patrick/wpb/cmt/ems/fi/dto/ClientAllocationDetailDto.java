package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAllocationDetailDto {

    List<ClientAllocationBreakdownDto> clientAllocationBreakdowns;
    List<ClientAllocationAmendLogDto> clientAllocationAmendLogs;
    List<RegionalAllocationBreakdownSummaryDto> regionalAllocationBreakdownSummaries;
}

