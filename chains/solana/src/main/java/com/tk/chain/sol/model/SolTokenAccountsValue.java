package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAccountsValue {

    @JSONField(name = "account")
    private SolTokenAccountValue account;

    @JSONField(name = "pubkey")
    private String pubkey;
}
