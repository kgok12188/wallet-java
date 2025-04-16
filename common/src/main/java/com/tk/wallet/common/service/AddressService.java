package com.tk.wallet.common.service;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.fingerprint.CalcFingerprintService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Resource
    private WalletAddressService walletAddressService;
    @Resource
    private WalletSymbolConfigService walletSymbolConfigService;
    @Resource
    private SymbolConfigService symbolConfigService;
    @Resource
    private WalletWithdrawService walletWithdrawService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private CalcFingerprintService calcFingerprintService;


    public String getAndSaveAddress(Integer walletId, String chainId, ChainScanConfig chainScanConfig, Long uid) {
        uid = uid == null ? 0L : uid;
        if (chainScanConfig == null) {
            return "";
        }
        if (StringUtils.isBlank(chainScanConfig.getAddressSymbol()) && uid > 0) {
            WalletAddress walletAddress = walletAddressService.lambdaQuery().eq(WalletAddress::getWalletId, walletId)
                    .eq(WalletAddress::getUid, uid)
                    .eq(WalletAddress::getBaseSymbol, chainId).last("limit 1").one();
            if (walletAddress != null) {
                return walletAddress.getAddress();
            }
        }
        String addressUrl = chainScanConfig.getAddressUrl();
        if (StringUtils.isBlank(addressUrl) && StringUtils.isNotBlank(chainScanConfig.getAddressSymbol())) {
            ChainScanConfig addressChainScanConfig = chainScanConfigService.getByChainId(chainScanConfig.getAddressSymbol());
            if (addressChainScanConfig != null) {
                addressUrl = addressChainScanConfig.getAddressUrl();
            }
        }
        if (StringUtils.isBlank(addressUrl)) {
            return "";
        }

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JSONObject> ret = restTemplate.postForEntity(addressUrl, new HashMap<>(), JSONObject.class);
            if (ret.getStatusCode().is2xxSuccessful() && ret.getBody() != null) {
                String address = ret.getBody().getString("address");
                if (uid > 0 && StringUtils.isNotBlank(chainScanConfig.getAddressSymbol())) {
                    this.save(address, walletId, chainScanConfig.getAddressSymbol(), uid);
                }
                this.save(address, walletId, chainId, uid);
                return address;
            }
        } catch (Exception e) {
            logger.error("get_address_error {} {}", chainId, addressUrl, e);
        }
        return "";
    }

    /**
     * @param address    地址
     * @param walletUser 商户uid
     * @param baseSymbol 链id
     */
    public void save(String address, Integer walletUser, String baseSymbol, Long uid) {
        WalletAddress walletAddress = new WalletAddress();
        walletAddress.setAddress(address);
        walletAddress.setWalletId(walletUser);
        walletAddress.setBaseSymbol(baseSymbol);
        walletAddress.setUseStatus(1);
        walletAddress.setCtime(new Date());
        walletAddress.setMtime(new Date());
        walletAddress.setUid(uid);
        walletAddressService.save(walletAddress);
        calcFingerprintService.calcFingerprint(walletAddress, walletAddressService, new WalletAddress());
    }

    public boolean owner(String fromAddress, String toAddress, String chainId, String hash) {
        List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().in(WalletAddress::getAddress,
                        Lists.newArrayList(fromAddress, toAddress))
                .eq(WalletAddress::getBaseSymbol, chainId).list();
        return CollectionUtils.isNotEmpty(walletAddresses);
    }

    public boolean owner(String address, String chainId) {
        List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, address).eq(WalletAddress::getBaseSymbol, chainId).list();
        return CollectionUtils.isNotEmpty(walletAddresses);
    }

}
