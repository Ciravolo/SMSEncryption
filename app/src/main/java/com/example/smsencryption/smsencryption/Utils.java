package com.example.smsencryption.smsencryption;

import android.util.Log;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;


/**
 * Created by joana on 6/28/17.
 */

public class Utils {


    public String generateNonce(){
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String nonceGenerated = new String(Hex.encodeHex(bytes));
        return nonceGenerated;
    }


    public byte[] deriveKey(String p, byte[] s, int i, int l) throws Exception {
        PBEKeySpec ks = new PBEKeySpec(p.toCharArray(), s, i, l);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(ks).getEncoded();
    }

    public ArrayList<String> divideMessageManyParts(String message){

        int chunks = message.length()/154;
        Log.i("quantity chunks: ",String.valueOf(chunks));

        float res = message.length() % 154;
        Log.i("residue chunks: ", String.valueOf(res));

        if (res>0){
            chunks = chunks + 1;
        }
        int len = message.length();

        int strl;

        Log.i("so now chunks:", String.valueOf(chunks));

        ArrayList<String> struct = new ArrayList<String>();

        for(int i=0; i<chunks; i++){
            strl = 154*i+ 154;
            if (strl>= len){
                strl = len;
            }
            struct.add(i,message.substring(i*154,strl));
        }

        return struct;
    }

    public static Map<String, Object> getRSAKeys() throws Exception{

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Map<String, Object> keys = new HashMap<String,Object>();
        keys.put("private", privateKey);
        keys.put("public", publicKey);

        String strPrivate = new String(Hex.encodeHex(privateKey.getEncoded()));
        String strPublic = new String(Hex.encodeHex(publicKey.getEncoded()));

        //print the obtained key pair
        Log.i("private key:", strPrivate);
        Log.i("public key:", strPublic);

        return keys;
    }

}
