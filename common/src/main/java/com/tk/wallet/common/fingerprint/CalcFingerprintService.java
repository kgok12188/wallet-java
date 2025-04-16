package com.tk.wallet.common.fingerprint;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CalcFingerprintService {

    protected static Logger logger = LoggerFactory.getLogger(CalcFingerprintService.class);

    public static final Map<String, String> keyMap = new ConcurrentHashMap<>();

    public static final String PUBLIC_KEY_URL = "/fingerprint/getPublicKey"; //get请求
    public static final String DECRYPT_URL = "/fingerprint/decrypt"; //post请求

    @Value("${fingerprint.salt:ad7dNBma}")
    private String salt;

    @Value("${fingerprint.url:http://127.0.0.1:5678}")
    private String encryptUrl;


    @SuppressWarnings("all")
    public <T extends CalcFingerprint> void calcFingerprint(T calcFingerprint, IService<T> service, T update) {
        String originStr = calcFingerprint.calcFingerprint(salt);
        String fingerprint = encrypt(originStr);
        calcFingerprint.setFingerprint(fingerprint);
        if (service != null) {
            update.setId(calcFingerprint.getId());
            update.setFingerprint(fingerprint);
            service.updateById(update);
        }
    }

    /*
     * 指纹校验
     * */
    public <T extends CalcFingerprint<?>> boolean matchFingerprint(T calcFingerprint) {
        String originStr = calcFingerprint.calcFingerprint(salt);
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> params = new HashMap<>();
        params.put("value", calcFingerprint.getFingerprint());
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(encryptUrl + DECRYPT_URL, params, JSONObject.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            return false;
        }
        return StringUtils.equals(originStr, response.getBody().getString("value"));
    }

    /**
     * 加密
     */
    private String encrypt(String fingerprint) {
        String publicKeyStr = getPublicKey();
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr.trim());
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(fingerprint.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("指纹加密失败：" + fingerprint);
        }
    }

    private String getPublicKey() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(encryptUrl + PUBLIC_KEY_URL, new HashMap<>(), JSONObject.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            return "";
        }
        return response.getBody().containsKey("value") ? response.getBody().getString("value") : "";
    }

}
