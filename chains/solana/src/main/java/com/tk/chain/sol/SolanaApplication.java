package com.tk.chain.sol;

import com.tk.chains.ScanChainBlock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {"com.tk.chain", "com.tk.wallet.common", "com.tk.chains"})
@Import(ScanChainBlock.class)
public class SolanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolanaApplication.class, args);
    }

}
