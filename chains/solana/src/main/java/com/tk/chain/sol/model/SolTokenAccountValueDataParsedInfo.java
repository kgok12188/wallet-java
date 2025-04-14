package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAccountValueDataParsedInfo {

    @JSONField(name = "isNative")
    private Boolean isNative;

    @JSONField(name = "mint")
    private String mint;

    @JSONField(name = "owner")
    private String owner;

    @JSONField(name = "state")
    private String state;

    @JSONField(name = "tokenAmount")
    private SolTokenAmount tokenAmount;
}
