package com.tk.chain.sol.core;


import java.io.Serializable;

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


    public Encoding getEncoding() {
        return encoding;
    }

    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public boolean isSkipPreFlight() {
        return skipPreFlight;
    }

    public void setSkipPreFlight(boolean skipPreFlight) {
        this.skipPreFlight = skipPreFlight;
    }
}
