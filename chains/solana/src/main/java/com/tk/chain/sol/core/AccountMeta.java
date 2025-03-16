package com.tk.chain.sol.core;

//@Getter
//@AllArgsConstructor
public class AccountMeta {

    private PublicKey publicKey;

    private boolean isSigner;

    private boolean isWritable;

    public AccountMeta(PublicKey publicKey, boolean isSigner, boolean isWritable) {
        this.publicKey = publicKey;
        this.isWritable = isWritable;
        this.isSigner = isSigner;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isSigner() {
        return isSigner;
    }

    public void setSigner(boolean signer) {
        isSigner = signer;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public void setWritable(boolean writable) {
        isWritable = writable;
    }
}