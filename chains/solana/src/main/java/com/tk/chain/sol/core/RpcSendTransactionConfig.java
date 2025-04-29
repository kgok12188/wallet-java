package com.tk.chain.sol.core;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RpcSendTransactionConfig implements Serializable {

    public enum Encoding {
        base64("base64");

        private String enc;

        Encoding(String enc) {
            this.enc = enc;
        }

        public String getEncoding() {
            return enc;
        }

    }

    private Encoding encoding = Encoding.base64;

    private boolean skipPreFlight = true;

}
