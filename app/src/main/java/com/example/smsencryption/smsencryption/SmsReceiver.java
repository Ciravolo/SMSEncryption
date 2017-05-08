package com.example.smsencryption.smsencryption;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by joana on 4/16/17.
 */

public class SmsReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        String decryptData = intent.getStringExtra("DATA_ENCRYPTED");
        String sharedKey = intent.getStringExtra("SHARED_KEY");
        String nonce = intent.getStringExtra("NONCE");

        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
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
/*
            if ((decryptData.compareTo("")!=0)&&(sharedKey.compareTo("")!=0)&&(nonce.compareTo("")!=0)){
             if (decryptData.compareTo("1")==0){
                 str = decrypt(sharedKey,nonce,decryptData);
             }
            }
            */
            //---display the new SMS message---
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }
    }

    public static String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
