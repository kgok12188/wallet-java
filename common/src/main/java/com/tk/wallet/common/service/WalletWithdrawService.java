package com.tk.wallet.common.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.entity.WalletWithdraw;
import com.tk.wallet.common.mapper.WalletWithdrawMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 提现任务 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2022-09-29
 */
@Service
public class WalletWithdrawService extends ServiceImpl<WalletWithdrawMapper, WalletWithdraw> {

    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService; // 商户的币种配置
    @Autowired
    private SymbolConfigService symbolConfigService;

    public boolean withdraw(Integer appId, String chainId, String coin, String to, BigDecimal amount, String reqId) {

        SymbolConfig withdrawConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId).eq(SymbolConfig::getSymbol, coin).one();
        SymbolConfig coinConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId).eq(SymbolConfig::getContractAddress, "").one();

        if (withdrawConfig == null || coinConfig == null) {
            return false;
        }

        // 当前提币的配置币种
        WalletSymbolConfig tokenSymbolConfig = walletSymbolConfigService.lambdaQuery()
                .eq(WalletSymbolConfig::getWalletId, appId)
                .eq(WalletSymbolConfig::getSymbolConfigId, withdrawConfig.getId())
                .one();

        // 主币上配置的归集地址
        WalletSymbolConfig coinWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                .eq(WalletSymbolConfig::getWalletId, appId)
                .eq(WalletSymbolConfig::getSymbolConfigId, coinConfig.getId()).one();


        if (tokenSymbolConfig == null || coinWalletSymbolConfig == null) {
            return false;
        }

        String aggAddress = coinWalletSymbolConfig.getAggAddress();
        WalletWithdraw walletWithdraw = new WalletWithdraw();
        walletWithdraw.setWalletId(appId);
        walletWithdraw.setAmount(amount);
        walletWithdraw.setAddressTo(to);
        walletWithdraw.setAddressFrom(aggAddress);
        walletWithdraw.setBaseSymbol(chainId);
        walletWithdraw.setTransId(reqId);
        walletWithdraw.setSymbol(coin);
        walletWithdraw.setCtime(new Date());
        walletWithdraw.setMtime(new Date());
        if (tokenSymbolConfig.autoUpChain()) { // 无需审核
            walletWithdraw.setStatus(2);
        } else {
            walletWithdraw.setStatus(0);
        }
        this.save(walletWithdraw);
        return true;
    }

}
