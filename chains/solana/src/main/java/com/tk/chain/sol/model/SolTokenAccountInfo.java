package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAccountInfo {

    @JSONField(name = "context")
    private SolContext context;

    @JSONField(name = "value")
    private SolTokenAccountValue value;
}
