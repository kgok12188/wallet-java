package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAmount {

    @JSONField(name = "amount")
    private String amount;

    @JSONField(name = "decimals")
    private Integer decimals;

    @JSONField(name = "uiAmount")
    private BigDecimal uiAmount = BigDecimal.ZERO;

    @JSONField(name = "uiAmountString")
    private String uiAmountString;
}
