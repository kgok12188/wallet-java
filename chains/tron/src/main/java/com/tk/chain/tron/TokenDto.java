package com.tk.chain.tron;

public class TokenDto {
    private String owner_address;
    private String contract_address;
    private String function_selector;
    private String parameter;
    private boolean visible;
    private long call_value;
    private long fee_limit;

    public String getOwner_address() {
        return owner_address;
    }

    public void setOwner_address(String owner_address) {
        this.owner_address = owner_address;
    }

    public String getContract_address() {
        return contract_address;
    }

    public void setContract_address(String contract_address) {
        this.contract_address = contract_address;
    }

    public String getFunction_selector() {
        return function_selector;
    }

    public void setFunction_selector(String function_selector) {
        this.function_selector = function_selector;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public long getCall_value() {
        return call_value;
    }

    public void setCall_value(long call_value) {
        this.call_value = call_value;
    }

    public long getFee_limit() {
        return fee_limit;
    }

    public void setFee_limit(long fee_limit) {
        this.fee_limit = fee_limit;
    }
}
