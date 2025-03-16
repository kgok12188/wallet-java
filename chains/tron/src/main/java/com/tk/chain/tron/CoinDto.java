package com.tk.chain.tron;

public class CoinDto {
    private String owner_address;
    private String to_address;
    private long amount;
    /**
     * 账户地址是否为 Base58check 格式, 默认为 false, 使用 HEX 地址
     * hex的话 ：ApiWrapper.toHex(Base58Check.base58ToBytes(transferReqDto.getFromAddress()))
     */
    private boolean visible;

    public String getOwner_address() {
        return owner_address;
    }

    public void setOwner_address(String owner_address) {
        this.owner_address = owner_address;
    }

    public String getTo_address() {
        return to_address;
    }

    public void setTo_address(String to_address) {
        this.to_address = to_address;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
