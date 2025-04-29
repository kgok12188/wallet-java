package com.tk.wallet.common.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.mapper.ChainScanConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ChainScanConfigService extends ServiceImpl<ChainScanConfigMapper, ChainScanConfig> {

    private static final Logger logger = LoggerFactory.getLogger(ChainScanConfigService.class);

    public List<ChainScanConfig> getOpen(String taskId) {
        return this.lambdaQuery().eq(ChainScanConfig::getStatus, 1).eq(ChainScanConfig::getTaskId, taskId).list();
    }

    public void updateBlockNumber(String chainId, BigInteger blockNumber) {
        this.baseMapper.updateBlockNumber(chainId, blockNumber);
    }

    public ChainScanConfig getByChainId(String chainId) {
        return this.getById(chainId);
    }

    public void taskUpdateTime(String taskId, String chainIds) {
        this.baseMapper.taskUpdateTime(taskId);
        this.baseMapper.replaceChainScanHosts(taskId, chainIds);
    }

    public Integer hosts(String chainIds) {
        this.baseMapper.deleteLostHost(new Date(System.currentTimeMillis() - (60 * 1000)));
        Integer hosts = this.baseMapper.hosts(chainIds);
        return (hosts == null || hosts < 1) ? 1 : hosts;
    }

    public List<String> updateTaskId(String taskId, int count, List<String> chainIds) {
        ArrayList<String> res = new ArrayList<>();
        if (count < 1) {
            return res;
        }
        List<ChainScanConfig> list = this.lambdaQuery()
                .lt(ChainScanConfig::getTaskUpdateTime, new Date(System.currentTimeMillis() - (60 * 1000))).eq(ChainScanConfig::getStatus, 1)
                .in(ChainScanConfig::getChainId, chainIds)
                .orderByAsc(ChainScanConfig::getChainId)
                .last(" limit " + count)
                .list();
        for (ChainScanConfig chainScanConfig : list) {
            Integer row = this.baseMapper.updateTaskId(taskId, chainScanConfig.getChainId(), new Date(System.currentTimeMillis() - (60 * 1000)));
            if (row != null && row > 0) {
                res.add(chainScanConfig.getChainId());
            }
        }
        return res;
    }

    public void removeTaskId(String chainId, String taskId) {
        this.baseMapper.removeTaskId(chainId, taskId);
    }

    public List<String> circuitBreaker() {
        return this.baseMapper.circuitBreaker();
    }

    public void saveCircuitBreaker(String chainId) {
        this.baseMapper.saveCircuitBreaker(chainId);
    }

    public Long hasUnFinish(String chainId) {
        Long rows = this.baseMapper.hasUnFinishValue(chainId);
        return rows == null ? 0 : rows;
    }
}
