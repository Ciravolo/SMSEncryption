package com.example.smsencryption.smsencryption;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.codec.binary.Base64;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by joana on 4/16/17.
 *
 * Class called on every receive of a message
 */

public class SmsReceiver extends BroadcastReceiver{

    private String privateKeyA="";
    private String privateKeyB="";
    private String nonceFromSenderB="";
    private String raw = "";
    private boolean sessionErrorKey = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";

        String action = intent.getAction();
        Log.i("Receiver", "Broadcast received: " + action);

        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");

           // boolean firstStep =(boolean) bundle.get("FIRST_STEP_SESSION_KEY");
            if (pdus!=null){
                msgs = new SmsMessage[pdus.length];

                if (msgs!=null){
                    for (int i = 0; i < msgs.length; i++) {

                        //this has to be only for android versions < 19
                        if (Build.VERSION.SDK_INT < 19) {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }else{
                            //check if this works because this is only for the case sdk >=19
                            msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                        }

                        raw += msgs[i].getMessageBody();

                        str += "SMS from " + msgs[i].getOriginatingAddress();
                        str += " :";
                        str += msgs[i].getMessageBody().toString();
                        str += "\n";
                    }
                }
            }

            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }

        if (!raw.equals("")){

            String[] arr = raw.split(":");
            if (arr!=null)
            {
                String encryptedMessage = arr[0];
                if (arr.length>1){
                    int stepProtocol = Integer.parseInt(arr[1]);

                    switch(stepProtocol){

                        case 1:
                            try {

                                obtainPrivateKeyFromSenderFirstStep(encryptedMessage);
                                byte[] xorSessionKey = xor(Constants.getPrivateKeyA().getBytes("UTF-8") , privateKeyB.getBytes("UTF-8"));
                                Constants.setSessionKeyA(new String(Base64.encodeBase64(xorSessionKey)));
                                if (Constants.getSessionKeyA().equals("")){
                                    //couldn't create the session key for A. send alert
                                    sessionErrorKey = true;
                                }

                            } catch (IllegalBlockSizeException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (NoSuchPaddingException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (BadPaddingException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (UnsupportedEncodingException e){
                                sessionErrorKey = true;
                                e.printStackTrace();
                            }
                            break;
                        case 2:
                            try {
                                obtainPrivateKeyFromSenderSecondStep(encryptedMessage);
                                byte[] xorSessionKey = xor(privateKeyA.getBytes("UTF-8") , Constants.getPrivateKeyB().getBytes("UTF-8"));
                                String s_test = new String(Base64.encodeBase64(xorSessionKey));
                                String test_2 = s_test;
                                Constants.setSessionKeyB(s_test);

                            } catch (NoSuchAlgorithmException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (BadPaddingException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (IllegalBlockSizeException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (NoSuchPaddingException e) {
                                sessionErrorKey = true;
                                e.printStackTrace();
                            } catch (UnsupportedEncodingException e){
                                sessionErrorKey = true;
                                e.printStackTrace();
                            }
                            break;
                        case 3:
                            //when the session has already been established, both parts should have the same session key set.
                            if (Constants.getSessionKeyA().equals("") && Constants.getSessionKeyB().equals("")){
                                //means everything is blank and session key hasnt been established: error
                                sessionErrorKey = true;
                            }else{
                                String key = Constants.getSessionKeyA().equals("") ? Constants.getSessionKeyB(): Constants.getSessionKeyA();

                                byte[] keyArray = new byte[0];

                                try {
                                    keyArray = key.getBytes("UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                MessageDigest sha = null;
                                try {
                                    sha = MessageDigest.getInstance("SHA-1");
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                keyArray = sha.digest(keyArray);
                                keyArray = Arrays.copyOf(keyArray, 16); // use only first 128 bit
                                SecretKeySpec secretKeySpec = new SecretKeySpec(keyArray, "AES");

                                String plaintext = null;
                                try {
                                    plaintext = decryptPrivateMessageWithSessionKey(secretKeySpec,str);
                                } catch (IllegalBlockSizeException e) {
                                    e.printStackTrace();
                                    sessionErrorKey = true;
                                } catch (NoSuchPaddingException e) {
                                    e.printStackTrace();
                                    sessionErrorKey = true;
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                    sessionErrorKey = true;
                                } catch (BadPaddingException e) {
                                    e.printStackTrace();
                                    sessionErrorKey = true;
                                }

                                Toast.makeText(context, "Decrypted message: "+ plaintext, Toast.LENGTH_SHORT).show();
                            }

                            break;

                    }

                    if (sessionErrorKey){
                        Toast.makeText(context, "Error: could not establish the session key.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "Success: Session Key established.", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }

    }


    private String decryptPrivateMessageWithSessionKey(SecretKeySpec key, String messageToDecrypt)
            throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {

        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

        try{
            byte[] original = cipher.doFinal(Base64.decodeBase64(messageToDecrypt.getBytes("UTF-8")));
            String decryptedMessage = new String(original, "UTF-8");
            return decryptedMessage;
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
        }

    }

    private static byte[] xor(byte[] a, byte[] b){
        byte[] result = new byte[Math.min(a.length, b.length)];

        int len = result.length;

        for (int i=0; i<result.length; i++){
            result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
        }
        return result;
    }


    private byte[] xorWithKey(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i%key.length]);
        }
        return out;
    }

    public String encode(String s, String key) {
        return base64Encode(xorWithKey(s.getBytes(), key.getBytes()));
    }

    public String decode(String s, String key) {
        return new String(xorWithKey(base64Decode(s), key.getBytes()));
    }

    private byte[] base64Decode(String s) {
        return Base64.decodeBase64(s);
    }

    private String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes).replaceAll("\\s", "");

    }

    public void obtainPrivateKeyFromSenderFirstStep(String str)
            throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {

        try {
            byte[] data = str.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] nonceArray = Arrays.copyOfRange(data,0,24);
            byte[] dataToDecryptArray =  Arrays.copyOfRange(data, 24, data.length);

            nonceFromSenderB = new String(nonceArray);
            Constants.setPinB(nonceFromSenderB);

            String stringToDecrypt = new String(dataToDecryptArray);
            decryptPrivateKeyFromSenderFirstStep(stringToDecrypt);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    public void obtainPrivateKeyFromSenderSecondStep(String str)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {

        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, Constants.getLongtermSharedKeySecret());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try{
            byte[] original = cipher.doFinal(Base64.decodeBase64(str.getBytes("UTF-8")));

            byte[] privateKeyBuffer = Arrays.copyOfRange(original, 0, 16);
            privateKeyA = new String(privateKeyBuffer, "UTF-8");

        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
    }


    public void decryptPrivateKeyFromSenderFirstStep(String decryptData)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {

        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, Constants.getLongtermSharedKeySecret());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try{
            byte[] original = cipher.doFinal(Base64.decodeBase64(decryptData.getBytes("UTF-8")));
            byte[] privateKeyBuffer = Arrays.copyOfRange(original, 0, 16);
            privateKeyB = new String(privateKeyBuffer, "UTF-8");
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }

    }
}
