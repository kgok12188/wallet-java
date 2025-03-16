package com.tk.wallet.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.mapper.SymbolConfigMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

;

/**
 * <p>
 * 全局币种配置表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2022-09-30
 */
@Service
public class SymbolConfigService extends ServiceImpl<SymbolConfigMapper, SymbolConfig> {


    public List<SymbolConfig> getList(LambdaQueryWrapper<SymbolConfig> queryWrapper, int start, int size) {
        queryWrapper.orderByDesc(SymbolConfig::getId);
        return this.page(new Page<>(start, size), queryWrapper).getRecords();
    }

    public List<SymbolConfig> querySymbolConfigList(String baseSymbol, String symbol, Integer status, Integer type) {
        LambdaQueryWrapper<SymbolConfig> lqw = new LambdaQueryWrapper<>();
        if (StringUtils.isBlank(baseSymbol) && StringUtils.isBlank(symbol) && status == null && type == null) {
            return null;
        }
        if (StringUtils.isNotBlank(baseSymbol)) {
            lqw.eq(SymbolConfig::getBaseSymbol, baseSymbol);
        }
        if (StringUtils.isNotBlank(symbol)) {
            lqw.eq(SymbolConfig::getSymbol, symbol);
        }
        if (status != null) {
            lqw.eq(SymbolConfig::getStatus, status);
        }
        return this.list(lqw);
    }


    public List<SymbolConfig> querySubSymbolConfig(String baseSymbol) {
        LambdaQueryWrapper<SymbolConfig> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SymbolConfig::getBaseSymbol, baseSymbol).ne(SymbolConfig::getContractAddress, "");
        return this.list(lqw);
    }


    public SymbolConfig querySymbolConfig(String baseSymbol, String symbol) {
        Optional<SymbolConfig> symbolConfigOptional = this.lambdaQuery()
                .eq(SymbolConfig::getSymbol, symbol)
                .eq(SymbolConfig::getBaseSymbol, baseSymbol)
                .orderByDesc(SymbolConfig::getId).last(" limit 1 ").oneOpt();
        if (symbolConfigOptional.isPresent()) {
            return symbolConfigOptional.get();
        }
        return null;
    }


    public SymbolConfig queryMainSymbolConfig(String serviceType) {
        LambdaQueryWrapper<SymbolConfig> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SymbolConfig::getStatus, 1)
                .eq(SymbolConfig::getContractAddress, "")
                .eq(SymbolConfig::getBaseSymbol, serviceType).orderByDesc(SymbolConfig::getId).last(" limit 1");
        return this.getOne(lqw);
    }


    public SymbolConfig querySymbolConfigByContract(String contractAddress) {
        LambdaQueryWrapper<SymbolConfig> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SymbolConfig::getStatus, 1)
                .eq(SymbolConfig::getContractAddress, contractAddress)
                .orderByDesc(SymbolConfig::getId).last(" limit 1");
        return this.getOne(lqw);
    }

}
