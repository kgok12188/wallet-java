package com.tk.chain.sol.programs;


//import com.chainup.wallet.api.chain.support.sol.solDto.core.AccountMeta;
//import com.chainup.wallet.api.chain.support.sol.solDto.core.PublicKey;
//import com.chainup.wallet.api.chain.support.sol.solDto.core.TransactionInstruction;

import com.tk.chain.sol.core.AccountMeta;
import com.tk.chain.sol.core.PublicKey;
import com.tk.chain.sol.core.TransactionInstruction;

import java.util.ArrayList;

import static org.bitcoinj.core.Utils.int64ToByteArrayLE;
import static org.bitcoinj.core.Utils.uint32ToByteArrayLE;

public class SystemProgram extends Program {
    public static final PublicKey PROGRAM_ID = new PublicKey("11111111111111111111111111111111");

    public static final int PROGRAM_INDEX_CREATE_ACCOUNT = 0;
    public static final int PROGRAM_INDEX_TRANSFER = 2;

    public static TransactionInstruction transfer(PublicKey fromPublicKey, PublicKey toPublickKey, long lamports) {
        ArrayList<AccountMeta> keys = new ArrayList<AccountMeta>();
        keys.add(new AccountMeta(fromPublicKey, true, true));
        keys.add(new AccountMeta(toPublickKey, false, true));

        // 4 byte instruction index + 8 bytes lamports
        byte[] data = new byte[4 + 8];
        uint32ToByteArrayLE(PROGRAM_INDEX_TRANSFER, data, 0);
        int64ToByteArrayLE(lamports, data, 4);

        return createTransactionInstruction(PROGRAM_ID, keys, data);
    }

    public static TransactionInstruction createAccount(PublicKey fromPublicKey, PublicKey newAccountPublikkey,
                                                       long lamports, long space, PublicKey programId) {
        ArrayList<AccountMeta> keys = new ArrayList<AccountMeta>();
        keys.add(new AccountMeta(fromPublicKey, true, true));
        keys.add(new AccountMeta(newAccountPublikkey, true, true));

        byte[] data = new byte[4 + 8 + 8 + 32];
        uint32ToByteArrayLE(PROGRAM_INDEX_CREATE_ACCOUNT, data, 0);
        int64ToByteArrayLE(lamports, data, 4);
        int64ToByteArrayLE(space, data, 12);
        System.arraycopy(programId.toByteArray(), 0, data, 20, 32);

        return createTransactionInstruction(PROGRAM_ID, keys, data);
    }


    public static TransactionInstruction createAccountForkJsSdk(PublicKey fromMainPublicKey, PublicKey newAccountPublikkey, PublicKey toOwnerPublicKey, PublicKey mintPublicKey) {
        ArrayList<AccountMeta> keys = new ArrayList<AccountMeta>();
        keys.add(new AccountMeta(fromMainPublicKey, true, true));
        keys.add(new AccountMeta(newAccountPublikkey, false, true));
        keys.add(new AccountMeta(toOwnerPublicKey, false, false));
        keys.add(new AccountMeta(mintPublicKey, false, false));
        keys.add(new AccountMeta(PROGRAM_ID, false, false));
        keys.add(new AccountMeta(new PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"), false, false));
        keys.add(new AccountMeta(new PublicKey("SysvarRent111111111111111111111111111111111"), false, false));

        return createTransactionInstruction(new PublicKey("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"), keys, new byte[0]);
    }
}
