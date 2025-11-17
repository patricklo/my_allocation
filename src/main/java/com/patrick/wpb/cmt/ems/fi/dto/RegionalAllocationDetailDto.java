package com.patrick.wpb.cmt.ems.fi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionalAllocationDetailDto {

    RegionalAllocationDto regionalAllocation;
    List<RegionalAllocationBreakdownDto> regionalAllocationBreakdowns;
    List<FinalPricedAllocationBreakdownDto> finalPricedAllocationBreakdowns;
    List<FinalRegionalAllocationDto> finalRegionalAllocations;
}

