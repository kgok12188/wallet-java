package com.tk.wallet.common.fingerprint;

import java.io.Serializable;

public interface CalcFingerprint extends Serializable {
    String calcFingerprint(String key);

    String getFingerprint();

    void setFingerprint(String fingerprint);
}
