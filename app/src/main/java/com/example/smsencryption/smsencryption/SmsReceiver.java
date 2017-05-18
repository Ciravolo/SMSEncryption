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

    private String raw = "";

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        String rawMessage = "";

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

            Log.i("Receiver", "Raw message: "+ raw);

            if(action.equals("my.action.string")){

                String step = intent.getExtras().getString("step_number");
                Log.i("Action string: STEP ", step);
                int stepNumber = Integer.parseInt(step);

                switch(stepNumber){

                    case 1:
                        try {

                            privateKeyB = obtainPrivateKeyFromSenderFirstStep(raw);
                            Constants.setSessionKeyA(encode(Constants.getPrivateKeyA() , privateKeyB));

                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
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
                        }
                        break;
                    case 3:
                            //when the message is sent and the protocol has been established in
                        //both sides
                        break;

                }
            }
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
