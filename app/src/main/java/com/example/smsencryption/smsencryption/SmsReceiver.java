package com.example.smsencryption.smsencryption;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.codec.binary.Base64;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by joana on 4/16/17.
 *
 * Class called on every receive of a message
 */

public class SmsReceiver extends BroadcastReceiver{

    private String privateKeyA="";
    private String privateKeyB="";
    private String nonceFromSenderB="";
    @Override
    public void onReceive(Context context, Intent intent) {

        //String first_step_session_key = intent.getStringExtra("FIRST_STEP_SESSION_KEY");
        //String second_step_session_key = intent.getStringExtra("SECOND_STEP_SESSION_KEY");

        String action = intent.getAction();
        Log.i("Receiver", "Broadcast received: " + action);

        if(action.equals("my.action.string")){
            String step = intent.getExtras().getString("step_number");
        }

        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");

           // boolean firstStep =(boolean) bundle.get("FIRST_STEP_SESSION_KEY");

            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {

                //this has to be only for android versions < 19
                if (Build.VERSION.SDK_INT < 19) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }else{
                    //check if this works because this is only for the case sdk >=19
                    msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                }

                str += "SMS from " + msgs[i].getOriginatingAddress();
                str += " :";
                str += msgs[i].getMessageBody().toString();
                str += "\n";
            }

//            if (first_step_session_key!=null) {
//                if (first_step_session_key.compareTo("1") == 0) {
/*
                    try {

                        //TODO Instead of returning a privateKey in the following method it would
                        // be better to obtain the value from the global variable

                        privateKeyB = obtainPrivateKeyFromSenderFirstStep(str);
                        Constants.setSessionKeyA(encode(Constants.getPrivateKeyA() , privateKeyB));

                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    }
  //              }
  //          }

  //          if (second_step_session_key != null) {
  //              if (second_step_session_key.compareTo("1") == 0) {

                    try {
                         //TODO Instead of returning a privateKey in the following method it would
                        // be better to obtain the value from the global variable

                        privateKeyA = obtainPrivateKeyFromSenderSecondStep(str);
                        Constants.setSessionKeyB(encode(privateKeyA , Constants.getPrivateKeyB()));

                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    }*/
   //             }
    //        }

            //---display the new SMS message---
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }
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



    public String obtainPrivateKeyFromSenderFirstStep(String str)
            throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {

        try {
            byte[] data = str.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(data);

            ByteBuffer nonce = ByteBuffer.wrap(data, 0,16);
            ByteBuffer dataToDecrypt = ByteBuffer.wrap(data, 16, buffer.array().length);

            nonceFromSenderB = new String(Base64.encodeBase64(nonce.array()));
            nonceFromSenderB.replace('+','-').replace('/','_');

            Constants.setPinB(nonceFromSenderB);

            String stringToDecrypt = new String(Base64.encodeBase64(dataToDecrypt.array()));
            stringToDecrypt.replace('+','-').replace('/','_');

            return decryptPrivateKeyFromSenderFirstStep(stringToDecrypt);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }


    public String obtainPrivateKeyFromSenderSecondStep(String str)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {

        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, Constants.getLongtermSharedKeySecret());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

        try{
            byte[] original = cipher.doFinal(str.getBytes("UTF-8"));

            ByteBuffer privateKeyBuffer = ByteBuffer.wrap(original, 0,16);

            privateKeyA = new String(Base64.encodeBase64(privateKeyBuffer.array()));
            privateKeyA.replace('+','-').replace('/','_');
            return privateKeyA;

        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
        }
    }


    public String decryptPrivateKeyFromSenderFirstStep(String decryptData)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {

        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, Constants.getLongtermSharedKeySecret());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

        try{
            byte[] original = cipher.doFinal(decryptData.getBytes("UTF-8"));

            ByteBuffer privateKeyBuffer = ByteBuffer.wrap(original, 0,16);

            privateKeyB = new String(Base64.encodeBase64(privateKeyBuffer.array()));
            privateKeyB.replace('+','-').replace('/','_');
            return privateKeyB;

        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
        }

    }
}
