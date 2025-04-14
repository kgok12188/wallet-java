package com.tk.proxy.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.service.AddressService;
import com.tk.wallet.common.service.ChainScanConfigService;
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

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;


    @PostMapping("/get")
    @ApiOperation("获取地址")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "appId", example = "实例id"),
            @ApiImplicitParam(value = "chain", example = "链"),
            @ApiImplicitParam(value = "sign", example = "签名"),
            @ApiImplicitParam(value = "time", example = "时间戳")
    })
    public R<String> getAddress(@RequestBody JSONObject params) {
        Integer appId = params.getInteger("appId");
        if (appId == null) {
            return R.fail("appId is null");
        }
        String chain = params.getString("chain");
        if (StringUtils.isBlank(chain)) {
            return R.fail("chain is null");
        }
        ChainScanConfig one = chainScanConfigService.lambdaQuery().eq(ChainScanConfig::getChainId, chain).last("limit 1").one();
        if (one == null || StringUtils.isBlank(one.getAddressUrl())) {
            return R.fail("chain is not exist");
        }
        String address = addressService.getAndSaveAddress(appId, chain, one);
        return R.success(address);
    }

}
