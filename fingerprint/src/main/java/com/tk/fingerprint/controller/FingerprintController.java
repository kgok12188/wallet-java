package com.tk.fingerprint.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fingerprint")
public class FingerprintController {

    private static final Logger logger = LoggerFactory.getLogger(FingerprintController.class);

    @Value("${publicKey:MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArSJFrB6/8fF18OTeqvsJxRdLxF/roTTt9+aAz9A38muI53fN5l6fOYnz+w+UzZVN8O8WFbx5T3tDe46FnJLZcsAe2LZg9EbaUwJT6pXG3KRXETFBF0jbsbcVM3/FaVGqcuD4z4eN4bNPeW9rRDgrFBk9/c6PyDPM+pChfYZTlBHyDN5NBpuyqMGQl44wo0VaH7hba9WsG8m7nHZ0ILzsUk/ynZ9vsa2/FUDtE1+ld+jGytktXwB8abmQ4etWogJwXsbf8AADrqJclVz3WL3ac+A/qK/Xj2l8jdBbpEB+pD5QB4MnTP3JTY4x+ocpV3SbSYS5MbBHCrgX6DmIXDfi3wIDAQAB}")
    private String publicKey;
    @Value("${privateKey:MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCtIkWsHr/x8XXw5N6q+wnFF0vEX+uhNO335oDP0Dfya4jnd83mXp85ifP7D5TNlU3w7xYVvHlPe0N7joWcktlywB7YtmD0RtpTAlPqlcbcpFcRMUEXSNuxtxUzf8VpUapy4PjPh43hs095b2tEOCsUGT39zo/IM8z6kKF9hlOUEfIM3k0Gm7KowZCXjjCjRVofuFtr1awbybucdnQgvOxST/Kdn2+xrb8VQO0TX6V36MbK2S1fAHxpuZDh61aiAnBext/wAAOuolyVXPdYvdpz4D+or9ePaXyN0FukQH6kPlAHgydM/clNjjH6hylXdJtJhLkxsEcKuBfoOYhcN+LfAgMBAAECggEBAIkogIel6Kw0fRn1Rs2viQLhvL16vAH/G5LrwhpgOyJKvK+bArxBM+TyfB3mLx1a7d80mYtov7YO0dw9gW4UDD5TI1zNxyteQjHGw1Ixi9RaQTu2iHl418sHXr1ouK0B7IWL4rEOE4G9evWaJiFqWf5XLPw4O0IAf5/1GmqGEwpWitQ1W+lrr2tlwi7fON/ngD/lddORSA+k4gEZHyu18vp5i5npg88ewuJlMXahSbIvQk1y87aWdvDM/q2c96DrWcJ61Z/uUfWXDnkR3WuIhjuCSfQa1KF30I3y5rZqRqN/J5ffIoUUeXxt+FpSt3lv5ZkzVqJia/Nnagi9Z+4EOjECgYEA4Unpyln3TBtrYeDFQ/Wa+XUDffI7Yj6efM2htreLdKbftZud8FmdakE3qQazwMSq5OT2BUYNZsUoEaY58xlI/MpIB2MVRs6nWpkYBGykugsaukMhhfqWwOUudg4Zu02lL00rb/68P27FbZryiqNWg1IxwRSXyFPI4OY4cUXedDkCgYEAxLxEoTYuxzWWh+sMhD/OcL568yfJPbR+0YhXHNs9tknIPfoYqdvZq0D52RjpK9Q1gK4AOP9X3x4CH31aRqKuFnGeCE0S4nKfA6lIc8fcvuYAGKCO9gLB1I4m6R2KS4gnPd+cU1puGZZnRbSnzneSKdl3+vNAxIt6VYN/ooCcf9cCgYAbm3Ih0NqEIgwLp36k8FD4ZsVxxqBOTrwfMRezC1T0i4p4d8Rn8qNepPVMKj0Wz0Ld38ziIo573IcneoY5awzNpYAkH5k43xQU/xO5XxLklX3F+3n9MBMPOkZyQxIWKNKoND2xcLbi0xEweD9mi9OLLiRYs8xRySqhnLSn9NboyQKBgFblXV7jgsfkqIeCT3X+hN2RlNbfxOhnV/iOwwNw5xIkBrkxRUGJZNxah8DXWu5L/hHHSTvjtNlM9N0Zzg0S/9fT+VxrRqMUw52nYOQa7Cq5hmNaT6rjzt9mplMjBBmWmtaedPrwH19X6meEgvYUJFAtyOvkE8B8Zt1shqytJ/LjAoGBANgC3rcnUSAKEF1CUQQ8hQZEV2HT8OppoBzRDLQtwp46J7lWc4vCeNx+2kazGLuL7IND3Zm2h/uuIme9JWxQvYW9AMqsmKwzwuPkYk4E2kQCT5jbVy5YmQaX6lNrBHopPjy5cuDWeKREs1HDDuWuSBb7CIDu+F4994NILNfkwZfO}")
    public String privateKey;

    @PostMapping("/decrypt")
    public Map<String, Object> get(@RequestBody JSONObject params) {
        String encryptedData = params.getString("value");
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("status", 0);
        try {
            String decrypt = decrypt(encryptedData);
            ret.put("value", decrypt);
        } catch (Exception e) {
            logger.warn("decrypt_error", e);
            ret.put("value", "");
            ret.put("status", 500);
        }
        return ret;
    }

    @PostMapping("/encrypt")
    public Map<String, Object> encrypt(@RequestBody JSONObject params) {
        String value = params.getString("value");
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("status", 0);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            byte[] publicKeyBytes = Base64.getDecoder().decode(this.publicKey.trim());
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes());
            String s = Base64.getEncoder().encodeToString(encryptedBytes);
            ret.put("data", s);
        } catch (Exception e) {
            ret.put("status", 500);
            logger.warn("encrypt_error", e);
        }
        return ret;
    }

    @PostMapping("/getPublicKey")
    public Map<String, Object> getPublicKey() {
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("value", publicKey);
        return ret;
    }

    public String decrypt(String encryptedData) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(this.privateKey);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

}
