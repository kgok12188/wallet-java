package com.tk.chains.controller;

import com.alibaba.fastjson.JSONObject;
import com.tk.chains.BlockChain;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import com.tk.wallet.common.service.WalletWithdrawService;
import com.tk.wallet.common.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService;
    @Autowired
    private SymbolConfigService symbolConfigService;
    @Autowired
    private WalletWithdrawService walletWithdrawService;


    @PostMapping("/loadHash")
    public R<List<ChainTransaction>> loadHash(@RequestBody JSONObject params) {
        String chainId = params.getString("chainId");
        String hash = params.getString("hash");
        BlockChain<?> blockChain = applicationContext.getBean(chainId, BlockChain.class);
        return R.success(blockChain.getChainTransaction(chainId, hash, null));
    }


    @PostMapping("/withdraw")
    public R<Boolean> withdraw(@RequestBody JSONObject params) {
        String chainId = params.getString("chainId");
        Integer walletId = params.getInteger("walletId");
        String symbol = params.getString("symbol");
        String toAddress = params.getString("to");
        BigDecimal amount = params.getBigDecimal("amount");
        String reqId = params.getString("reqId");
        SymbolConfig symbolConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId)
                .eq(SymbolConfig::getStatus, 1).last("limit 1").one();
        if (symbolConfig == null) {
            return R.fail("symbolConfig is null");
        }
        walletWithdrawService.withdraw(walletId, chainId, symbol, toAddress, amount, reqId);
        return R.success(true);
    }


}
