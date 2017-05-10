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
    public static final String PRIVATE_KEY_A = "privatekeyA";
    public static final String PRIVATE_KEY_B = "privatekeyB";
    public static String PIN_A = "";
    public static String PIN_B = "";
}
