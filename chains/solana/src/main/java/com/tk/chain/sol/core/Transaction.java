package com.tk.chain.sol.core;

import com.tk.chain.sol.utils.ShortvecEncoding;
import com.tk.chain.sol.utils.TweetNaclFast;
import org.bitcoinj.core.Base58;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Transaction {

    public static final int SIGNATURE_LENGTH = 64;

    private Message message;
    private List<String> signatures;
    private byte[] serializedMessage;

    public Transaction() {
        this.message = new Message();
        this.signatures = new ArrayList<String>();
    }

    public Transaction addInstruction(TransactionInstruction instruction) {
        message.addInstruction(instruction);

        return this;
    }

    public void setRecentBlockHash(String recentBlockhash) {
        message.setRecentBlockHash(recentBlockhash);
    }

    public void sign(Account signer) {
        sign(Arrays.asList(signer));
    }

    public void sign(List<Account> signers) {

        if (signers.size() == 0) {
            throw new IllegalArgumentException("No signers");
        }

        Account feePayer = signers.get(0);
        message.setFeePayer(feePayer);

        serializedMessage = message.serialize();
        System.out.println("payload:" + Base58.encode(serializedMessage));

        for (Account signer : signers) {
            TweetNaclFast.Signature signatureProvider = new TweetNaclFast.Signature(new byte[0], signer.getSecretKey());
            byte[] signature = signatureProvider.detached(serializedMessage);
            System.out.println("signature:" + Base58.encode(signature));
            System.out.println("signatureHex:" + bytesToHex(signature));
            signatures.add(Base58.encode(signature));
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public void serializeOnly(List<Account> signers) {
        if (signers.size() == 0) {
            throw new IllegalArgumentException("No signers");
        }
        Account feePayer = signers.get(0);
        message.setFeePayer(feePayer);
        serializedMessage = message.serialize();
    }

    public String getRawMessage(List<Account> signers) {
        if (signers.size() == 0) {
            throw new IllegalArgumentException("No signers");
        }
        Account feePayer = signers.get(0);
        message.setFeePayer(feePayer);
        serializedMessage = message.serialize();
        System.out.println("payload:" + Base58.encode(serializedMessage));
        return Base58.encode(serializedMessage);
    }

    public void setSignature(String signature) {
        signatures.add(Base58.encode(Hex.decode(signature)));
    }

    public byte[] serialize() {
        int signaturesSize = signatures.size();
        byte[] signaturesLength = ShortvecEncoding.encodeLength(signaturesSize);

        ByteBuffer out = ByteBuffer
                .allocate(signaturesLength.length + signaturesSize * SIGNATURE_LENGTH + serializedMessage.length);

        out.put(signaturesLength);

        for (String signature : signatures) {
            byte[] rawSignature = Base58.decode(signature);
            out.put(rawSignature);
        }

        out.put(serializedMessage);
        byte[] result = out.array();
        System.out.println("serialize:" + Base58.encode(result));
        return result;
    }
}
