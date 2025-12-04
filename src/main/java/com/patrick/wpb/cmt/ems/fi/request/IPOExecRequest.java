package com.patrick.wpb.cmt.ems.fi.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IPOExecRequest extends OrderMatchAndExecRequest {

    @NotEmpty(message = "Regional counterparty execution request list cannot be empty")
    @Valid
    private List<RegionalCounterpartyExecutionRequest> regionalCounterpartyExecutionRequestList;
}

