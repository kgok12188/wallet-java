package com.tk.chain.sol.core;

import cn.hutool.core.codec.Base58;
import com.tk.chain.sol.utils.TweetNaclFast;
import com.tk.chain.sol.wallet.DerivableType;
import com.tk.chain.sol.wallet.SolanaBip44;
import org.bitcoinj.crypto.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Account {
    private TweetNaclFast.Signature.KeyPair keyPair;

    public Account() {
        this.keyPair = TweetNaclFast.Signature.keyPair();
    }

    public Account(byte[] secretKey) {
        this.keyPair = TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);
    }

    public Account(String address) {
        this.keyPair = new TweetNaclFast.Signature.KeyPair();
        keyPair.setPublicKey(Base58.decode(address));
    }

    private Account(TweetNaclFast.Signature.KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Deprecated
    public static Account fromMnemonic(List<String> words, String passphrase) {
        byte[] seed = MnemonicCode.toSeed(words, passphrase);
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);
        DeterministicKey child = deterministicHierarchy.get(HDUtils.parsePath("M/501H/0H/0/0"), true, true);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(child.getPrivKeyBytes());
        return new Account(keyPair);
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(new Account().getSecretKey()));
    }

    /**
     * Derive a Solana account from a Mnemonic generated by the Solana CLI using bip44 Mnemonic with deviation path of
     * m/55H/501H/0H
     *
     * @param words      seed words
     * @param passphrase seed passphrase
     * @return Solana account
     */
    public static Account fromBip44Mnemonic(List<String> words, String passphrase) {
        SolanaBip44 solanaBip44 = new SolanaBip44();
        byte[] seed = MnemonicCode.toSeed(words, passphrase);
        byte[] privateKey = solanaBip44.getPrivateKeyFromSeed(seed, DerivableType.BIP44);
        return new Account(TweetNaclFast.Signature.keyPair_fromSeed(privateKey));
    }

    /**
     * Derive a Solana account from a Mnemonic generated by the Solana CLI using bip44 Mnemonic with deviation path of
     * m/55H/501H/0H/0H
     *
     * @param words      seed words
     * @param passphrase seed passphrase
     * @return Solana account
     */
    public static Account fromBip44MnemonicWithChange(List<String> words, String passphrase) {
        SolanaBip44 solanaBip44 = new SolanaBip44();
        byte[] seed = MnemonicCode.toSeed(words, passphrase);
        byte[] privateKey = solanaBip44.getPrivateKeyFromSeed(seed, DerivableType.BIP44CHANGE);
        return new Account(TweetNaclFast.Signature.keyPair_fromSeed(privateKey));
    }

    /**
     * Derive a Solana account from a Mnemonic generated by the Solana CLI
     *
     * @param words      seed words
     * @param passphrase seed passphrase
     * @return Solana account
     */
    public static Account fromBip39Mnemonic(List<String> words, String passphrase) {
        byte[] seed = MnemonicCode.toSeed(words, passphrase);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(seed);

        return new Account(keyPair);
    }

    /**
     * Creates an {@link Account} object from a Sollet-exported JSON string (array)
     *
     * @param json Sollet-exported JSON string (array)
     * @return {@link Account} built from Sollet-exported private key
     */
    public static Account fromJson(String json) {
        return new Account(convertJsonStringToByteArray(json));
    }

    public PublicKey getPublicKey() {
        return new PublicKey(keyPair.getPublicKey());
    }

    public byte[] getSecretKey() {
        return keyPair.getSecretKey();
    }

    /**
     * Convert's a Sollet-exported JSON string into a byte array usable for {@link Account} instantiation
     *
     * @param characters Sollet-exported JSON string
     * @return byte array usable in {@link Account} instantiation
     */
    private static byte[] convertJsonStringToByteArray(String characters) {
        // Create resulting byte array
        ByteBuffer buffer = ByteBuffer.allocate(64);

        // Convert json array into String array
        String sanitizedJson = characters.replaceAll("\\[", "").replaceAll("]", "");
        String[] chars = sanitizedJson.split(",");

        // Convert each String character into byte and put it in the buffer
        Arrays.stream(chars).forEach(character -> {
            byte byteValue = (byte) Integer.parseInt(character);
            buffer.put(byteValue);
        });

        return buffer.array();
    }
}