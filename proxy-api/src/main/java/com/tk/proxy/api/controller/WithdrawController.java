package com.tk.proxy.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.service.ChainTransactionService;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletWithdrawService;
import com.tk.wallet.common.vo.R;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/withdraw")
public class WithdrawController {

    @Autowired
    private SymbolConfigService symbolConfigService;
    @Autowired
    private ChainTransactionService chainTransactionService;
    @Autowired
    private WalletWithdrawService walletWithdrawService;

    @PostMapping("/add")
    @ApiOperation("提现")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "appId", example = "实例id"),
            @ApiImplicitParam(value = "chainId", example = "链id"),
            @ApiImplicitParam(value = "coin", example = "代币名称"),
            @ApiImplicitParam(value = "from", example = "from地址"),
            @ApiImplicitParam(value = "to", example = "to地址"),
            @ApiImplicitParam(value = "businessId", example = "业务id")
    })
    public R<Boolean> add(@RequestBody JSONObject params) {
        String chainId = params.getString("chainId");
        String coin = params.getString("coin");
        String to = params.getString("to");
        String reqId = params.getString("reqId");
        BigDecimal amount = params.getBigDecimal("amount");
        Integer appId = params.getInteger("appId");
        SymbolConfig one = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId).eq(SymbolConfig::getTokenSymbol, coin).one();
        if (one == null) {
            return R.fail("tokenSymbol is null");
        }
        boolean withdraw = walletWithdrawService.withdraw(appId, chainId, coin, to, amount, reqId);
        return R.success(withdraw);
    }

}
