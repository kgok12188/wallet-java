package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolBalance {

    @JSONField(name = "context")
    private SolContext context;

    @JSONField(name = "value")
    private BigDecimal value;
}
