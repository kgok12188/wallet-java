package com.tk.wallet.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.mapper.WalletSymbolConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 币种配置表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2022-10-08
 */
@Service
public class WalletSymbolConfigService extends ServiceImpl<WalletSymbolConfigMapper, WalletSymbolConfig> {
    public List<WalletSymbolConfig> getList(LambdaQueryWrapper<WalletSymbolConfig> queryWrapper, int start, int size) {
        queryWrapper.orderByDesc(WalletSymbolConfig::getId);
        return this.page(new Page<>(start, size), queryWrapper).getRecords();
    }


    public List<WalletSymbolConfig> querySymbolConfigList(Integer symbolConfigId, Integer walletId, Integer status, Integer aggPolice) {
        LambdaQueryWrapper<WalletSymbolConfig> lqw = new LambdaQueryWrapper<>();
        if (symbolConfigId == null && walletId == null && status == null) {
            return null;
        }
        if (symbolConfigId != null) {
            lqw.eq(WalletSymbolConfig::getSymbolConfigId, symbolConfigId);
        }
        if (walletId != null) {
            lqw.eq(WalletSymbolConfig::getWalletId, walletId);
        }
        if (status != null) {
            lqw.eq(WalletSymbolConfig::getStatus, status);
        }
        if (aggPolice != null) {
            lqw.eq(WalletSymbolConfig::getAggPolice, aggPolice);
        }
        return this.list(lqw);
    }


    public List<Integer> distinctWalletIds() {
        return this.baseMapper.distinctWalletIds();
    }

}
