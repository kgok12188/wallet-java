package com.tk.wallet.common.fingerprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CalcFingerprintService {

    protected static Logger logger = LoggerFactory.getLogger(CalcFingerprintService.class);

    public static final Map<String, String> keyMap = new ConcurrentHashMap<>();

    public static final String PUBLIC_KEY_PREFIX = "public_key_prefix_";
    // public static final String COMMON_ENCRYPT_BASE_URL = "common_encrypt_base_url";
    public static final String GET_PUB_KEY_URL = "/key/pub_key"; //get请求
    public static final String ENCRYPT_URL = "/core/encrypt"; //post请求

    @Value("${stats_withdraw_finger_print_key:ad7dNBma}")
    private String stats_withdraw_finger_print_key;

    @Value("${common_encrypt_base_url:http://ex.host:8328}")
    private String encryptUrl;

    public String calcFingerprint(CalcFingerprint calcFingerprint) {
        String originStr = calcFingerprint.calcFingerprint(stats_withdraw_finger_print_key);
        return encrypt(originStr);
    }

    public void updateFingerprint(CalcFingerprint calcFingerprint) {
        String originStr = calcFingerprint.calcFingerprint(stats_withdraw_finger_print_key);
        calcFingerprint.setFingerprint(encrypt(originStr));
    }

    /*
     * 指纹校验
     * */
    public boolean matchFingerprint(CalcFingerprint calcFingerprint) {
        return matchFingerprint(calcFingerprint, null);
    }

    /*
     * 指纹校验
     * */
    public boolean matchFingerprint(CalcFingerprint calcFingerprint, String message) {
        return true;
    }

    /**
     * 加密
     */
    private String encrypt(String fingerprint) {
        return fingerprint;
    }

    public String encrypt(int type, int source, String hash) {
        return hash;
    }

    public String getPublicKey(int type, int source) {
        return "";
    }

}
