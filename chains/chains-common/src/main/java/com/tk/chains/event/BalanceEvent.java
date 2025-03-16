package com.tk.chains.event;


import com.tk.wallet.common.entity.CoinBalance;

public class BalanceEvent extends Event {

    private CoinBalance balance;

    public BalanceEvent(CoinBalance balance) {
        this.balance = balance;
    }

    public CoinBalance getBalance() {
        return balance;
    }

    public void setBalance(CoinBalance balance) {
        this.balance = balance;
    }
}
