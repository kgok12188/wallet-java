package com.tk.chain.sol.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressBalance {

    private String address;

    private BigDecimal balance;

    private String lockedBalance;

    private String symbol;
}
