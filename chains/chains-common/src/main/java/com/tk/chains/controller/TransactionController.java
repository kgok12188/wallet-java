package com.tk.chains.controller;

import com.alibaba.fastjson.JSONObject;
import com.tk.chains.BlockChain;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private ApplicationContext applicationContext;


    @PostMapping("/loadHash")
    public R<List<ChainTransaction>> loadHash(@RequestBody JSONObject params) {
        String chainId = params.getString("chainId");
        String hash = params.getString("hash");
        BlockChain<?> blockChain = applicationContext.getBean(chainId, BlockChain.class);
        return R.success(blockChain.getChainTransaction(chainId, hash, null));
    }


}
