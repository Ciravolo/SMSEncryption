package com.example.smsencryption.smsencryption;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.codec.binary.Base64;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private ArrayList<String> multiPartMessage;
    private String infoToDecrypt = "";
    private String raw = "";
    private boolean sessionErrorKey = false;
    private String originatingPhoneNumber="";
    private int indexMultipart=0;

    private String errorReason="";

    String SENT_SMS_FLAG = "SENT_SMS_FLAG";
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

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
                        originatingPhoneNumber = msgs[i].getOriginatingAddress();

                        str += "SMS from " + msgs[i].getOriginatingAddress();
                        str += " :";
                        str += msgs[i].getMessageBody().toString();
                        str += "\n";
                    }
                }
            }

            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        }

        if (!raw.equals("")) {

            if (raw.contains(":")) {

                String[] arr = raw.split(":");
                if (arr != null) {
                    String receivedMessage = arr[0];

                    if (arr.length > 1) {

                        String protocolId = arr[1];

                        int stepProtocol = 0;

                        if (arr.length > 2)
                            stepProtocol = Integer.parseInt(arr[2]);

                        if (protocolId != null) {
                            if (protocolId.compareTo("P") == 0) {
                                //run the protocol for public key exchange
                                switch (stepProtocol) {
                                    case 0:

                                        //I'm receiving the nonce so I set it
                                        Constants.setHisNonce(receivedMessage);

                                        //Now I generate my nonce
                                        Utils u = new Utils();
                                        String myNonce = u.generateNonce();

                                        //I set my nonce
                                        Constants.setMyNonce(myNonce);

                                        try {
                                            //I get a pair of keys for RSA to set my public key/private key
                                            Map<String, Object> keys = u.getRSAKeys();

                                            PrivateKey privateKey = (PrivateKey) keys.get("private");
                                            PublicKey publicKey = (PublicKey) keys.get("public");

                                            Constants.setMyPrivateKey(privateKey);
                                            Constants.setMyPublicKey(publicKey);

                                            byte[] bytesMyPublicKey = Constants.getMyPublicKey().getEncoded();

                                            String strMyPublicKey = new String(Base64.encodeBase64(bytesMyPublicKey));
                                            strMyPublicKey.replace('+', '-').replace('/', '_');

                                            //concat his nonce with my public key

                                            byte[] firstPartWithoutEnc = (Constants.getHisNonce() + strMyPublicKey).getBytes("UTF-8");

                                            Log.i("Step 0: My nonce:",Constants.getMyNonce());
                                            Log.i("Step 0: His nonce:",Constants.getHisNonce());

                                            byte[] salt = generateHashFromNonces(Constants.getHisNonce(), Constants.getMyNonce());

                                            String saltStr = new String(Base64.encodeBase64(salt));
                                            saltStr.replace('+', '-').replace('/', '_');

                                            Log.i("Step 0: W =", Constants.getW());
                                            Log.i("Step 0: Salt =", saltStr);

                                            //byte[] keyForExchangeKeys = u.generateKeyHKDF(strSalt, Constants.getW());
                                            byte[] keyForExchangeKeys = u.deriveKey(Constants.getW(), salt, 1, 128);

                                            Constants.setKeyForExchangeKeys(keyForExchangeKeys);

                                            //TODO: check if the key is the same that is being generated
                                            String strKeyForExchange = new String(Base64.encodeBase64(keyForExchangeKeys));
                                            strKeyForExchange.replace('+', '-').replace('/', '_');

                                            Log.i("Step 0: Key exchange:", strKeyForExchange);

                                            String encryptedStringFirstPart = encryptSymmetric(firstPartWithoutEnc, keyForExchangeKeys);

                                            String finalStringToSend = Constants.getMyNonce() + encryptedStringFirstPart + ":P:1";
                                            Log.i("Step 0: finalString", finalStringToSend);
                                            //send the finalString via sms
                                            SmsManager smsManager = SmsManager.getDefault();

                                            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                    intent, 0);
                                            context.getApplicationContext().registerReceiver(
                                                    new SmsReceiver(),
                                                    new IntentFilter(SENT_SMS_FLAG));


                                            if (finalStringToSend.length()>160){

                                                ArrayList<String> parts = u.divideMessageManyParts(finalStringToSend);

                                                for (int i=0; i< parts.size()-1; i++){
                                                    parts.set(i, parts.get(i)+":P:1");
                                                }

                                                for (int j=0; j< parts.size(); j++){
                                                    parts.set(j, parts.size()+"*"+parts.get(j));
                                                }

                                                for (int k=0; k<parts.size(); k++){
                                                    smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                            parts.get(k), sentIntent, null);
                                                }

                                                //ArrayList<String> parts = smsManager.divideMessage(finalStringToSend);
                                                //smsManager.sendMultipartTextMessage(originatingPhoneNumber, null, parts, null, null);
                                            }
                                            else{
                                                smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                        finalStringToSend, sentIntent, null);
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            sessionErrorKey = true;
                                            errorReason = e.getMessage();
                                        }
                                        break;
                                    case 1:
                                        try {

                                            if (arr.length > 2) {

                                                String[] arrSplit = arr[0].split("\\*");
                                                multiPartMessage = new ArrayList<String>(Integer.parseInt(arrSplit[0]));
                                                multiPartMessage.add(indexMultipart, arrSplit[1]);
                                                indexMultipart++;

                                                Constants.setNumberMessages(Constants.getNumberMessages()+1);
                                                Constants.setDecryptionMessage(Constants.getDecryptionMessage()+arrSplit[1]);

                                                if (Integer.parseInt(arrSplit[0]) == Constants.getNumberMessages()) {

                                                    infoToDecrypt = Constants.getDecryptionMessage();

                                                    if (!infoToDecrypt.isEmpty()) {
                                                        //obtain nonce from sender

                                                        Log.i("text to decrypt:", Constants.getDecryptionMessage());

                                                        //byte[] receivedBytes = infoToDecrypt.getBytes("UTF-8");
                                                        byte[] receivedBytes = Base64.decodeBase64(infoToDecrypt.getBytes("UTF-8"));
                                                        byte[] hisNoncePart = Arrays.copyOf(receivedBytes, 16);

                                                        String strHisNonce = new String(Base64.encodeBase64(hisNoncePart));

                                                        Constants.setHisNonce(strHisNonce);

                                                        //Salt: calculate the hash given 2 nonces

                                                        Log.i("Step 1: My nonce:",Constants.getMyNonce());
                                                        Log.i("Step 1: His nonce:",Constants.getHisNonce());

                                                        byte[] salt = generateHashFromNonces(Constants.getMyNonce(), Constants.getHisNonce());

                                                        //To check if the salt is the same
                                                        String strSalt = new String(Base64.encodeBase64(salt));
                                                        strSalt.replace('+', '-').replace('/', '_');

                                                        Utils u2 = new Utils();

                                                        Log.i("Step 1: W =", Constants.getW());
                                                        Log.i("Step 1: Salt=", strSalt);

                                                        byte[] keyForExchangeKeys = u2.deriveKey(Constants.getW(), salt, 1, 128);

                                                        //TODO: check if the key is the same that is being generated
                                                        String strKeyForExchange = new String(Base64.encodeBase64(keyForExchangeKeys));
                                                        strKeyForExchange.replace('+', '-').replace('/', '_');

                                                        Log.i("Step 1: Key exchange:", strKeyForExchange);

                                                        Constants.setKeyForExchangeKeys(keyForExchangeKeys);

                                                        byte[] obtainedMyNonce = Arrays.copyOfRange(receivedBytes, 0, 16);

                                                        //TODO: checking what is the value sent here for the nonce
                                                        String obtainedNonceStr = new String(Base64.encodeBase64(obtainedMyNonce));
                                                        obtainedNonceStr.replace('+', '-').replace('/', '_');

                                                        Log.i("Step 1: obtained nonce:", obtainedNonceStr);

                                                        byte[] toDecryptPart = Arrays.copyOfRange(receivedBytes, 16, receivedBytes.length);
                                                        String decryptedMessage = decryptSymmetric(toDecryptPart, keyForExchangeKeys);



                                                        //compare my nonce with the one in the decryption
                                                        byte[] decryptedMessageInBytes = decryptedMessage.getBytes("UTF-8");
                                                        byte[] myNoncePart = Arrays.copyOfRange(decryptedMessageInBytes, 0, 16);

                                                        //passing to string this nonce and comparing with the nonce I have set before
                                                        String myNoncePartString = new String(Base64.encodeBase64(myNoncePart));
                                                        myNoncePartString.replace('+', '-').replace('/', '_');

                                                        if (Constants.getMyNonce().compareTo(myNoncePartString) == 0) {
                                                            //meaning that the message from Bob is alright and I can obtain the public key from B
                                                            byte[] publicKeyPart = Arrays.copyOfRange(decryptedMessageInBytes, 16, decryptedMessageInBytes.length);
                                                            // I create the public key for B and I set it

                                                            PublicKey hisPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyPart));
                                                            Constants.setHisPublicKey(hisPublicKey);

                                                            //generate my keys
                                                            //I get a pair of keys for RSA to set my public key/private key
                                                            Map<String, Object> keys = u2.getRSAKeys();

                                                            PrivateKey myPrivateKey = (PrivateKey) keys.get("private");
                                                            PublicKey myPublicKey = (PublicKey) keys.get("public");

                                                            Constants.setMyPrivateKey(myPrivateKey);
                                                            Constants.setMyPublicKey(myPublicKey);

                                                            //prepare the message to be sent in the 3rd step of the Public key exchange protocol

                                                            byte[] bytesMyPublicKey = Constants.getMyPublicKey().getEncoded();

                                                            String strMyPublicKey = new String(Base64.encodeBase64(bytesMyPublicKey));
                                                            strMyPublicKey.replace('+', '-').replace('/', '_');

                                                            byte[] bytesHisPublicKey = Constants.getHisPublicKey().getEncoded();

                                                            String strHisPublicKey = new String(Base64.encodeBase64(bytesHisPublicKey));
                                                            strHisPublicKey.replace('+', '-').replace('/', '_');

                                                            //concat all the strings to form the final to encrypt message

                                                            String messageToEncrypt = Constants.getHisNonce() + strMyPublicKey + strHisPublicKey;

                                                            byte[] messageBytesToEncrypt = messageToEncrypt.getBytes("UTF-8");
                                                            //encrypt this string
                                                            String finalMessage = encryptSymmetric(messageBytesToEncrypt, keyForExchangeKeys);

                                                            finalMessage = finalMessage + ":P:2";

                                                            //send the message to receiver
                                                            SmsManager smsManager = SmsManager.getDefault();

                                                            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                                    intent, 0);
                                                            context.getApplicationContext().registerReceiver(
                                                                    new SmsReceiver(),
                                                                    new IntentFilter(SENT_SMS_FLAG));
                                                            smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                                    finalMessage, sentIntent, null);


                                                        } else {
                                                            sessionErrorKey = true;
                                                            errorReason = "Message received in second step has no correspondence to the protocol";
                                                        }
                                                    } else {
                                                        sessionErrorKey = true;
                                                        errorReason = "Info to decrypt is null";
                                                    }

                                                }

                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            sessionErrorKey = true;
                                            errorReason = e.getMessage();
                                        }
                                        //longTermSharedKeyString = Arrays.copyOf(longTermSharedKeyString, 16);

                                        break;
                                    case 2:
                                        try {
                                            //decrypt the message received with the already set key for exchange
                                            byte[] receivedBytes = receivedMessage.getBytes("UTF-8");
                                            String decryptedMessage = decryptSymmetric(receivedBytes, Constants.getKeyForExchangeKeys());

                                            byte[] decryptedBytes = decryptedMessage.getBytes("UTF-8");
                                            byte[] noncePart = Arrays.copyOfRange(decryptedBytes, 0, 16);

                                            //need to compare that the noncePart corresponds to my nonce
                                            String noncePartReceived = new String(Base64.encodeBase64(noncePart));
                                            noncePartReceived.replace('+', '-').replace('/', '_');

                                            if (noncePartReceived.compareTo(Constants.getMyNonce()) == 0) {

                                                byte[] publicKeyA = Arrays.copyOfRange(decryptedBytes, 16, 2064);
                                                byte[] publicKeyB = Arrays.copyOfRange(decryptedBytes, 2064, 4112);

                                                //from byte[] to public key and compare again if it corresponds to my already set key

                                                PublicKey myPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyB));

                                                if (myPublicKey == Constants.getMyPublicKey()) {

                                                    PublicKey hisPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyA));
                                                    Constants.setHisPublicKey(hisPublicKey);

                                                    //now send message for confirmation of the protocol

                                                    String messageToBeSent = encryptSymmetric(Constants.getHisPublicKey().getEncoded(), Constants.getKeyForExchangeKeys());

                                                    messageToBeSent = messageToBeSent + ":P:3";

                                                    //send the final confirmation message to receiver
                                                    SmsManager smsManager = SmsManager.getDefault();

                                                    PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                            intent, 0);
                                                    context.getApplicationContext().registerReceiver(
                                                            new SmsReceiver(),
                                                            new IntentFilter(SENT_SMS_FLAG));
                                                    smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                            messageToBeSent, sentIntent, null);

                                                } else {
                                                    errorReason = "Public key sent does not correspond to the one in the receiver.";
                                                    sessionErrorKey = true;
                                                }

                                            } else {
                                                errorReason = "The nonce sent does not correspond to the one in the receiver.";
                                                sessionErrorKey = true;
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            sessionErrorKey = true;
                                            errorReason = e.getMessage();
                                        }

                                        break;
                                    case 3:

                                        try {
                                            //decrypt the message received with the already set key for exchange
                                            byte[] receivedBytes = receivedMessage.getBytes("UTF-8");
                                            String decryptedMessage = decryptSymmetric(receivedBytes, Constants.getKeyForExchangeKeys());
                                            byte[] decryptedBytes = decryptedMessage.getBytes("UTF-8");
                                            PublicKey receivedPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decryptedBytes));

                                            //I need to compare if it corresponds to my own public key
                                            if (receivedPublicKey == Constants.getMyPublicKey()) {
                                                //success, the protocol has been established
                                                SmsManager smsManager = SmsManager.getDefault();

                                                PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                        intent, 0);
                                                context.getApplicationContext().registerReceiver(
                                                        new SmsReceiver(),
                                                        new IntentFilter(SENT_SMS_FLAG));
                                                smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                        "Success! Protocol has been set", sentIntent, null);
                                            } else {
                                                errorReason = "Public key sent does not correspond to the receivers public key";
                                                sessionErrorKey = true;
                                            }

                                        } catch (Exception e) {

                                        }

                                        break;
                                }
                            } else {
                                if (protocolId.compareTo("E") == 0) {
                                    //run protocol from the message exchange encryption
                                } else {
                                    if (protocolId.compareTo("W") == 0) {
                                        //received the initial W
                                        Constants.setW(receivedMessage);
                                    }
                                }
                            }
                        }



/*
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
                                    plaintext = decryptPrivateMessageWithSessionKey(secretKeySpec,encryptedMessage);
                                    String test = plaintext;
                                    String tt = test;

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

                                if (plaintext!=null){
                                    Toast.makeText(context, "Decrypted message: "+ plaintext, Toast.LENGTH_SHORT).show();
                                } else {
                                    sessionErrorKey = true;
                                }
                            }

                            break;

                    }
*/
                        if (sessionErrorKey) {
                            Toast.makeText(context, errorReason, Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }
        }
    }


    public String encryptSymmetric(byte[] message, byte[] key) {

        try{

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Constants.setLongTermSharedKeySecret(secretKeySpec);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encrypted = cipher.doFinal(message);

            String strEncrypted = new String(Base64.encodeBase64(encrypted));
            strEncrypted.replace('+','-').replace('/','_');

            return strEncrypted;
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String decryptSymmetric(byte[] message, byte[] key) throws
            NoSuchAlgorithmException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException{

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Constants.setLongTermSharedKeySecret(secretKeySpec);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

        try{
            byte[] original = cipher.doFinal(message);
            byte[] oo = original;

            String decryptedMessage = new String(original, "UTF-8");
            String dd = decryptedMessage;

            return decryptedMessage;
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
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
            byte[] oo = original;

            String decryptedMessage = new String(original, "UTF-8");
            String dd = decryptedMessage;
            
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

    public byte[] generateHashFromNonces(String hisNonce, String myNonce) throws
            UnsupportedEncodingException, NoSuchAlgorithmException{

        byte[] saltHash = (myNonce + hisNonce).getBytes("UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        saltHash = sha.digest(saltHash);
        saltHash = Arrays.copyOf(saltHash, 16); // use only first 128 bit

        return saltHash;
    }
}
