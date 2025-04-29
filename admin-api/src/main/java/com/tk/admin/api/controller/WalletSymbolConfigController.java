package com.tk.admin.api.controller;

import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.fingerprint.CalcFingerprintService;
import com.tk.wallet.common.service.AddressService;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import com.tk.wallet.common.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/walletSymbolConfig")
public class WalletSymbolConfigController {

    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService;
    @Autowired
    private SymbolConfigService symbolConfigService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private CalcFingerprintService calcFingerprintService;

    @PostMapping("/add")
    @Transactional
    public R<Boolean> add(@RequestBody WalletSymbolConfig walletSymbolConfig) {
        walletSymbolConfig.setId(null);
        WalletSymbolConfig dbConfig = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, walletSymbolConfig.getWalletId())
                .eq(WalletSymbolConfig::getSymbolConfigId, walletSymbolConfig.getSymbolConfigId())
                .last("limit 1")
                .one();
        if (dbConfig != null) {
            return R.success(true);
        }
        SymbolConfig symbolConfig = symbolConfigService.getById(walletSymbolConfig.getSymbolConfigId());
        if (symbolConfig == null) {
            return R.fail("参数错误SymbolConfigId");
        }
        Integer walletId = walletSymbolConfig.getWalletId();
        if (walletId == null) {
            return R.fail("参数错误WalletId");
        }
        if (StringUtils.isBlank(walletSymbolConfig.getColdAddress()) && StringUtils.isBlank(symbolConfig.getContractAddress())) {
            return R.fail("参数错误ColdAddress");
        }

        SymbolConfig mainSymbolConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, symbolConfig.getBaseSymbol())
                .eq(SymbolConfig::getContractAddress, "").one();

        WalletSymbolConfig mainWalletSymbolConfig = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, walletId)
                .eq(WalletSymbolConfig::getSymbolConfigId, mainSymbolConfig.getId()).one();

        if (mainWalletSymbolConfig == null && StringUtils.isNotBlank(symbolConfig.getContractAddress())) { // 配置代币，需要先配置主币
            return R.fail("请先配置主币");
        }

        if (mainWalletSymbolConfig != null && StringUtils.isNotBlank(symbolConfig.getContractAddress())) {
            walletSymbolConfig.setAggAddress(mainWalletSymbolConfig.getAggAddress());
            walletSymbolConfig.setEnergyAddress(mainWalletSymbolConfig.getEnergyAddress());
            walletSymbolConfig.setWithdrawAddress(mainWalletSymbolConfig.getWithdrawAddress());
        } else {
            ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(symbolConfig.getBaseSymbol());
            String address = addressService.getAndSaveAddress(walletId, symbolConfig.getBaseSymbol(), chainScanConfig, 0L);
            if (StringUtils.isBlank(address)) {
                return R.fail("服务异常");
            }
            walletSymbolConfig.setAggAddress(address);
            walletSymbolConfig.setEnergyAddress(address);
            walletSymbolConfig.setWithdrawAddress(address);
        }
        walletSymbolConfigService.save(walletSymbolConfig);
        calcFingerprintService.calcFingerprint(walletSymbolConfig, walletSymbolConfigService, new WalletSymbolConfig());
        return R.success(true);
    }

}
