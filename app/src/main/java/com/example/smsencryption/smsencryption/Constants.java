package com.example.smsencryption.smsencryption;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * Class to declare all the constants shared by the application in all activities
 * Created by joana on 5/10/17.
 */

public class Constants {

    public static final String LONGTERM_SHARED_KEY = "secretlongterm12";
    public static SecretKeySpec LONGTERM_SHARED_KEY_SECRET;
    public static String SESSION_KEY_A = "";
    public static String SESSION_KEY_B = "";
    public static final String PRIVATE_KEY_A = "privatekeyA12345";
    public static final String PRIVATE_KEY_B = "privatekeyB12345";
    public static String PIN_A = "";
    public static String PIN_B = "";

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

    public static String getPrivateKeyB(){
        return PRIVATE_KEY_B;
    }

    public static String getPrivateKeyA(){
        return PRIVATE_KEY_A;
    }

    public static void setLongTermSharedKeySecret(SecretKeySpec s){
        LONGTERM_SHARED_KEY_SECRET = s;
    }

    public static SecretKeySpec getLongtermSharedKeySecret(){
        return LONGTERM_SHARED_KEY_SECRET;
    }

    public static String getLongTermSharedKey(){
        return LONGTERM_SHARED_KEY;
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
