package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAccountValueData {

    @JSONField(name = "parsed")
    private SolTokenAccountValueDataParsed parsed;

    @JSONField(name = "program")
    private String program;

    @JSONField(name = "space")
    private BigInteger space;
}
