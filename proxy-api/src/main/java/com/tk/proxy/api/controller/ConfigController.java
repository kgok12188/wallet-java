package com.tk.proxy.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.service.AddressService;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import com.tk.wallet.common.vo.R;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class ConfigController {

    @Autowired
    private SymbolConfigService symbolConfigService;
    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private AddressService addressService;

    @PostMapping("/api/v1/coins")
    @ApiOperation("获取支持的所有币种")
    @ApiImplicitParams({@ApiImplicitParam(value = "pid", example = "项目编号")})
    public R<List<Map<String, Object>>> tokens(@RequestBody JSONObject params) {
        Integer walletId = params.getInteger("pid");
        List<WalletSymbolConfig> list = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, walletId).list();
        List<Map<String, Object>> coins = new ArrayList<>();
        for (WalletSymbolConfig walletSymbolConfig : list) {
            SymbolConfig symbolConfig = symbolConfigService.getById(walletSymbolConfig.getSymbolConfigId());
            if (symbolConfig != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("coin_name", symbolConfig.getSymbol());
                item.put("chain_id", symbolConfig.getBaseSymbol());
                item.put("token_id", StringUtils.isBlank(symbolConfig.getContractAddress()) ? symbolConfig.getBaseSymbol() : symbolConfig.getContractAddress());
                coins.add(item);
            }
        }
        return R.success(coins);
    }

    @PostMapping("/api/v1/chains")
    @ApiOperation("获取支持的所有链")
    @ApiImplicitParams({@ApiImplicitParam(value = "pid", example = "项目编号")})
    public R<Set<String>> getChains(@RequestBody JSONObject params) {
        Integer walletId = params.getInteger("pid");
        if (walletId == null) {
            return R.success(chainScanConfigService.lambdaQuery().list().stream().map(ChainScanConfig::getChainId).collect(Collectors.toSet()));
        } else {
            List<SymbolConfig> symbolConfigs = symbolConfigService.lambdaQuery().inSql(SymbolConfig::getId, "select symbol_config_id from wallet_symbol_config where wallet_id = " + walletId).list();
            return R.success(symbolConfigs.stream().map(SymbolConfig::getBaseSymbol).collect(Collectors.toSet()));
        }
    }

    @PostMapping("/api/v1/address/create")
    @ApiOperation("创建地址")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "pid", example = "项目编号"),
            @ApiImplicitParam(value = "chain_id", example = "链编号chain_id")
    })
    public R<Map<String, String>> addressCreate(@RequestBody JSONObject params) {
        Integer walletId = params.getInteger("pid");
        String chainId = params.getString("chain_id");
        addressService.getAndSaveAddress(walletId, chainId, chainScanConfigService.getByChainId(chainId), 0L);
        return null;
    }

}
