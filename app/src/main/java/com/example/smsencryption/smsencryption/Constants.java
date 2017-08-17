package com.example.smsencryption.smsencryption;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
/**
 *
 * Class to declare all the constants shared by the application in all activities
 * Created by joana on 5/10/17.
 */

public class Constants {

    private static SecretKey LONGTERM_SHARED_KEY_SECRET;

    private static String W;

    private static String SESSION_KEY_A = "";
    private static String SESSION_KEY_B = "";
    private static String PRIVATE_KEY_A = "privatekeyA12345";
    private static String PRIVATE_KEY_B = "privatekeyB12345";

    private static String PIN_A = "";
    private static String PIN_B = "";

    private static String myNonce = "";
    private static String hisNonce = "";

    private static PublicKey myPublicKey;
    private static PrivateKey myPrivateKey;

    private static PublicKey hisPublicKey;

    private static byte[] keyForExchangeKeys;

    private static String decryptionMessage = "";

    private static int numberMessages = 0;

    public static int getNumberMessages(){
        return numberMessages;
    }

    public static void setNumberMessages(int n){
        numberMessages = n;
    }

    public static void setDecryptionMessage(String message){
        decryptionMessage = message;
    }

    public static String getDecryptionMessage(){
        return decryptionMessage;
    }

    public static void setKeyForExchangeKeys(byte[] k){
        keyForExchangeKeys = k;
    }

    public static void setW(String w){ W = w;}

    public static void setMyNonce(String pin){ myNonce= pin; }

    public static void setHisNonce(String pin){ hisNonce = pin; }

    public static void setHisPublicKey(PublicKey key){
        hisPublicKey = key;
    }

    public static void setMyPublicKey(PublicKey key){
        myPublicKey = key;
    }

    public static void setMyPrivateKey(PrivateKey key){
        myPrivateKey = key;
    }

    public static byte[] getKeyForExchangeKeys(){ return keyForExchangeKeys; }

    public static PublicKey getMyPublicKey() { return myPublicKey; }

    public static PrivateKey getMyPrivateKey() { return myPrivateKey; }

    public static PublicKey getHisPublicKey(){ return hisPublicKey; }

    public static void setPinA(String pin){
        PIN_A = pin;
    }

    public static void setPinB(String pin){
        PIN_B = pin;
    }

    public static String getPinA(){
        return PIN_A;
    }

    public static String getPinB(){
        return PIN_B;
    }

    public static String getMyNonce(){
        return myNonce;
    }

    public static String getHisNonce(){
        return hisNonce;
    }

    public static String getW(){ return W; }

    public static String getPrivateKeyB(){
        return PRIVATE_KEY_B;
    }

    public static String getPrivateKeyA(){
        return PRIVATE_KEY_A;
    }

    public static void setLongTermSharedKeySecret(SecretKey s){
        LONGTERM_SHARED_KEY_SECRET = s;
    }

    public static SecretKey getLongtermSharedKeySecret(){
        return LONGTERM_SHARED_KEY_SECRET;
    }

    public static void setSessionKeyA(String sessionKeyA){
        SESSION_KEY_A = sessionKeyA;
    }

    public static void setSessionKeyB(String sessionKeyB){
        SESSION_KEY_B= sessionKeyB;
    }

    public static String getSessionKeyA(){
        return SESSION_KEY_A;
    }

    public static String getSessionKeyB(){
        return SESSION_KEY_B;
    }
}
