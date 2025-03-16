package com.tk.proxy.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tk.proxy.api", "com.tk.wallet.common"})
public class ProxyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyApiApplication.class, args);
    }

}
