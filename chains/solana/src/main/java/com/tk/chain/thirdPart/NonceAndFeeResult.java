package com.tk.chain.thirdPart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NonceAndFeeResult {
    @JsonProperty("nonce")
    private Long nonce;
    @JsonProperty("totalFees")
    private Long totalFees;
    @JsonProperty("code")
    private Long code;
}