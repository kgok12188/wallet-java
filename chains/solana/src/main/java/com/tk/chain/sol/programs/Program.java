package com.tk.chain.sol.programs;
//
//import com.chainup.wallet.api.chain.support.sol.solDto.core.AccountMeta;
//import com.chainup.wallet.api.chain.support.sol.solDto.core.PublicKey;
//import com.chainup.wallet.api.chain.support.sol.solDto.core.TransactionInstruction;

import com.tk.chain.sol.core.AccountMeta;
import com.tk.chain.sol.core.PublicKey;
import com.tk.chain.sol.core.TransactionInstruction;

import java.util.List;

public abstract class Program {


    public static TransactionInstruction createTransactionInstruction(
            PublicKey programId,
            List<AccountMeta> keys,
            byte[] data
    ) {
        return new TransactionInstruction(programId, keys, data);
    }
}
