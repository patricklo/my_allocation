package com.patrick.wpb.cmt.ems.fi.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionalCounterpartyExecutionRequest {

    @NotBlank(message = "Country code is required")
    private String countryCode;

    private String counterpartyId;
}
