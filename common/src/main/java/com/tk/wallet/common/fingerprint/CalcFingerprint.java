package com.tk.wallet.common.fingerprint;

import java.io.Serializable;

public interface CalcFingerprint<T extends Serializable> extends Serializable {

    void setId(T id);

    T getId();

    String calcFingerprint(String key);

    String getFingerprint();

    void setFingerprint(String fingerprint);

}
