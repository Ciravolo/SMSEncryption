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
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
            if (pdus != null) {
                msgs = new SmsMessage[pdus.length];

                if (msgs != null) {
                    for (int i = 0; i < msgs.length; i++) {

                        //this has to be only for android versions < 19
                        if (Build.VERSION.SDK_INT < 19) {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        } else {
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
                                        Constants.setHisNonce(receivedMessage);
                                        Utils u = new Utils();
                                        String myNonce = u.generateNonce();
                                        Constants.setMyNonce(myNonce);

                                        Log.i("My nonce:", Constants.getMyNonce());
                                        Log.i("His nonce:", Constants.getHisNonce());

                                        try {
                                            //I get a pair of keys for RSA to set my public key/private key
                                            Map<String, Object> keys = u.getRSAKeys();

                                            //Generation of public and private keys on Bob
                                            PrivateKey privateKey = (PrivateKey) keys.get("private");
                                            PublicKey publicKey = (PublicKey) keys.get("public");

                                            Constants.setMyPrivateKey(privateKey);
                                            Constants.setMyPublicKey(publicKey);

                                            byte[] bytesMyPublicKey = Constants.getMyPublicKey().getEncoded();

                                            byte[] bytesHisNonce = Hex.decodeHex(Constants.getHisNonce().toCharArray());

                                            byte[] firstPartWithoutEnc = new byte[bytesHisNonce.length + bytesMyPublicKey.length];
                                            System.arraycopy(bytesHisNonce, 0, firstPartWithoutEnc, 0, bytesHisNonce.length);
                                            System.arraycopy(bytesMyPublicKey, 0, firstPartWithoutEnc, bytesHisNonce.length, bytesMyPublicKey.length);

                                            byte[] salt = generateHashFromNonces(Constants.getHisNonce(), Constants.getMyNonce());
                                            byte[] keyForExchangeKeys = u.deriveKey(Constants.getW(), salt, 1, 128);

                                            Constants.setKeyForExchangeKeys(keyForExchangeKeys);

                                            String strKeyForExchange = new String(Hex.encodeHex(keyForExchangeKeys));
                                            Log.i("KEY ON ENC:::", strKeyForExchange);

                                            String encryptedStringFirstPart = encryptSymmetric(firstPartWithoutEnc, keyForExchangeKeys);

                                            String finalStringToSend = Constants.getMyNonce() + encryptedStringFirstPart + ":P:1";


                                            //clear the variables to be reused on next transmission
                                            Constants.setNumberMessages(0);
                                            Constants.setDecryptionMessage("");

                                            SmsManager smsManager = SmsManager.getDefault();

                                            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                    intent, 0);
                                            context.getApplicationContext().registerReceiver(
                                                    new SmsReceiver(),
                                                    new IntentFilter(SENT_SMS_FLAG));

                                            if (finalStringToSend.length() > 160) {

                                                ArrayList<String> parts = u.divideMessageManyParts(finalStringToSend);

                                                for (int i = 0; i < parts.size() - 1; i++) {
                                                    parts.set(i, parts.get(i) + ":P:1");
                                                }

                                                //At the beginning sending an indicator for the quantity of msgs to be sent
                                                //(e.g. if there are 4 messages: 4*---- message---)
                                                for (int j = 0; j < parts.size(); j++) {
                                                    parts.set(j, parts.size() + "*" + parts.get(j));
                                                }
                                                //sending the messages to the recipient: Alice
                                                for (int k = 0; k < parts.size(); k++) {
                                                    smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                            parts.get(k), sentIntent, null);
                                                }
                                            } else {
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

                                            //When alice receives the encrypted data first step
                                            if (arr.length > 2) {

                                                String[] arrSplit = arr[0].split("\\*");

                                                Constants.setNumberMessages(Constants.getNumberMessages() + 1);
                                                Constants.setDecryptionMessage(Constants.getDecryptionMessage() + arrSplit[1]);

                                                if (Integer.parseInt(arrSplit[0]) == Constants.getNumberMessages()) {

                                                    infoToDecrypt = Constants.getDecryptionMessage();

                                                    if (!infoToDecrypt.isEmpty()) {

                                                        byte[] receivedBytes = Hex.decodeHex(infoToDecrypt.toCharArray());
                                                        byte[] hisNoncePart = Arrays.copyOfRange(receivedBytes, 0, 16);
                                                        byte[] toDecryptPart = Arrays.copyOfRange(receivedBytes, 16, receivedBytes.length);

                                                        String strHisNonce = new String(Hex.encodeHex(hisNoncePart));

                                                        Log.i("His Nonce:", strHisNonce);
                                                        Log.i("My Nonce:", Constants.getMyNonce());
                                                        Constants.setHisNonce(strHisNonce);

                                                        byte[] salt = generateHashFromNonces(Constants.getMyNonce(), Constants.getHisNonce());

                                                        Utils u2 = new Utils();

                                                        byte[] keyForExchangeKeys = u2.deriveKey(Constants.getW(), salt, 1, 128);

                                                        String strDecKey = new String(Hex.encodeHex(keyForExchangeKeys));
                                                        Log.i("KEY ON DEC:::", strDecKey);

                                                        String strToDecrypt = new String(Hex.encodeHex(toDecryptPart));
                                                        Log.i("string to dec::", strToDecrypt);

                                                        Constants.setKeyForExchangeKeys(keyForExchangeKeys);
                                                        String decryptedMessage = decryptSymmetric(toDecryptPart, keyForExchangeKeys);

                                                        byte[] decryptedBytes = Hex.decodeHex(decryptedMessage.toCharArray());
                                                        byte[] myNoncePart = Arrays.copyOfRange(decryptedBytes, 0, 16);

                                                        String myNoncePartString = new String(Hex.encodeHex(myNoncePart));

                                                        Log.i("myNoncePartString:", myNoncePartString);

                                                        if (myNoncePartString.compareTo(Constants.getMyNonce()) == 0) {
                                                            //meaning that the message from Bob is alright and I can obtain the public key from B

                                                            byte[] publicKeyPart = Arrays.copyOfRange(decryptedBytes, 16, decryptedBytes.length);

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
                                                            String strMyPublicKey = new String(Hex.encodeHex(bytesMyPublicKey));
                                                            Log.i("SEND: mypubkey:", strMyPublicKey);

                                                            byte[] bytesHisPublicKey = Constants.getHisPublicKey().getEncoded();
                                                            String strHisPublicKey = new String(Hex.encodeHex(bytesHisPublicKey));
                                                            Log.i("SEND: hispubkey:", strHisPublicKey);

                                                            String messageToEncrypt = Constants.getHisNonce() + strMyPublicKey + strHisPublicKey;
                                                            byte[] messageBytesToEncrypt = Hex.decodeHex(messageToEncrypt.toCharArray());

                                                            //TODO: Calculate length without the encryption

                                                            String message = new String(Hex.encodeHex(messageBytesToEncrypt));
                                                            Log.i("SENDING: ", message);

                                                            String finalMessage = encryptSymmetric(messageBytesToEncrypt, keyForExchangeKeys);
                                                            finalMessage = finalMessage + ":P:2";
                                                            Log.i("Final msg for step 2:", finalMessage);


                                                            //clear the variables to be reused on next transmission
                                                            Constants.setNumberMessages(0);
                                                            Constants.setDecryptionMessage("");

                                                            //send the message to receiver
                                                            SmsManager smsManager = SmsManager.getDefault();

                                                            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                                    intent, 0);
                                                            context.getApplicationContext().registerReceiver(
                                                                    new SmsReceiver(),
                                                                    new IntentFilter(SENT_SMS_FLAG));

                                                            if (finalMessage.length() > 160) {

                                                                ArrayList<String> parts = u2.divideMessageManyParts(finalMessage);

                                                                for (int i = 0; i < parts.size() - 1; i++) {
                                                                    parts.set(i, parts.get(i) + ":P:2");
                                                                }

                                                                for (int j = 0; j < parts.size(); j++) {
                                                                    parts.set(j, parts.size() + "*" + parts.get(j));
                                                                }

                                                                for (int k = 0; k < parts.size(); k++) {
                                                                    smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                                            parts.get(k), sentIntent, null);
                                                                }
                                                            } else {
                                                                smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                                        finalMessage, sentIntent, null);
                                                            }


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

                                        break;
                                    case 2:
                                        try {
                                            if (arr.length > 2) {

                                                String[] arrSplit = arr[0].split("\\*");

                                                Constants.setNumberMessages(Constants.getNumberMessages() + 1);
                                                Constants.setDecryptionMessage(Constants.getDecryptionMessage() + arrSplit[1]);

                                                if (Integer.parseInt(arrSplit[0]) == Constants.getNumberMessages()) {

                                                    infoToDecrypt = Constants.getDecryptionMessage();

                                                    if (!infoToDecrypt.isEmpty()) {

                                                        byte[] receivedBytes = Hex.decodeHex(infoToDecrypt.toCharArray());

                                                        String strReceived = new String(Hex.encodeHex(receivedBytes));

                                                        Log.i("Step 2: msg received:", strReceived);

                                                        String decryptedMessage = decryptSymmetric(receivedBytes, Constants.getKeyForExchangeKeys());

                                                        byte[] decryptedBytes = Hex.decodeHex(decryptedMessage.toCharArray());

                                                        Log.i("Size of decr bytes:", String.valueOf(decryptedBytes.length));

                                                        byte[] noncePart = Arrays.copyOfRange(decryptedBytes, 0, 16);

                                                        String noncePartReceived = new String(Hex.encodeHex(noncePart));

                                                        Log.i("Nonce part received:", noncePartReceived);

                                                        if (noncePartReceived.compareTo(Constants.getMyNonce()) == 0) {

                                                            Log.i("info:", "nonce part received corresponds!");

                                                            byte[] publicKeyA = Arrays.copyOfRange(decryptedBytes, 16, 310);
                                                            byte[] publicKeyB = Arrays.copyOfRange(decryptedBytes, 310, 604);

                                                            //TODO: check if the obtained pub keys correspond

                                                            String strPubKeyA = new String(Hex.encodeHex(publicKeyA));
                                                            String strPubKeyB = new String(Hex.encodeHex(publicKeyB));

                                                            //todo: print it out
                                                            Log.i("Pub key A obtained:", strPubKeyA);
                                                            Log.i("Pub key B obtained:", strPubKeyB);

                                                            String myPubKey = new String(Hex.encodeHex(Constants.getMyPublicKey().getEncoded()));

                                                            //from byte[] to public key and compare again if it corresponds to my already set key

                                                            PublicKey myPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyB));

                                                            if (strPubKeyB.compareTo(myPubKey)==0) {

                                                                PublicKey hisPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyA));
                                                                Constants.setHisPublicKey(hisPublicKey);

                                                                String messageToBeSent = encryptSymmetric(Constants.getHisPublicKey().getEncoded(), Constants.getKeyForExchangeKeys());

                                                                messageToBeSent = messageToBeSent + ":P:3";

                                                                Log.i("before sending step 3:", messageToBeSent);

                                                                //clear the variables to be reused on next transmission
                                                                Constants.setNumberMessages(0);
                                                                Constants.setDecryptionMessage("");

                                                                Utils u3 = new Utils();
                                                                //send the message to receiver
                                                                SmsManager smsManager = SmsManager.getDefault();

                                                                PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                                                                        intent, 0);
                                                                context.getApplicationContext().registerReceiver(
                                                                        new SmsReceiver(),
                                                                        new IntentFilter(SENT_SMS_FLAG));

                                                                if (messageToBeSent.length() > 160) {

                                                                    ArrayList<String> parts = u3.divideMessageManyParts(messageToBeSent);

                                                                    for (int i = 0; i < parts.size() - 1; i++) {
                                                                        parts.set(i, parts.get(i) + ":P:3");
                                                                    }

                                                                    for (int j = 0; j < parts.size(); j++) {
                                                                        parts.set(j, parts.size() + "*" + parts.get(j));
                                                                    }

                                                                    for (int k = 0; k < parts.size(); k++) {
                                                                        smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                                                parts.get(k), sentIntent, null);
                                                                    }
                                                                } else {
                                                                    smsManager.sendTextMessage(originatingPhoneNumber, null,
                                                                            messageToBeSent, sentIntent, null);
                                                                }


                                                            } else {
                                                                errorReason = "Step 2: Public key sent does not correspond to the one in the receiver.";
                                                                sessionErrorKey = true;
                                                            }

                                                        } else {
                                                            errorReason = "Step 2: The nonce sent does not correspond to the one in the receiver.";
                                                            sessionErrorKey = true;
                                                        }
                                                    }

                                                }
                                            }
                                        } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        } catch (DecoderException e) {
                                            e.printStackTrace();
                                        } catch (InvalidKeySpecException e) {
                                            e.printStackTrace();
                                        }

                                        break;

                                    case 3:
                                        try {
                                            if (arr.length > 2) {
                                                Log.i("info1:", "here at least!");

                                                String[] arrSplit = arr[0].split("\\*");
                                                Log.i("get number of msgs:", String.valueOf(Constants.getNumberMessages()));

                                                Constants.setNumberMessages(Constants.getNumberMessages() + 1);
                                                Constants.setDecryptionMessage(Constants.getDecryptionMessage() + arrSplit[1]);
                                                Log.i("info2:", "here at least!");
                                                if (Integer.parseInt(arrSplit[0]) == Constants.getNumberMessages()) {

                                                    infoToDecrypt = Constants.getDecryptionMessage();

                                                    Log.i("info3:", "here at least!");
                                                    if (!infoToDecrypt.isEmpty()) {

                                                        byte[] receivedBytes = Hex.decodeHex(infoToDecrypt.toCharArray());

                                                        String strReceived = new String(Hex.encodeHex(receivedBytes));
                                                        Log.i("Step 3: msg received:", strReceived);

                                                        String decryptedMessage = decryptSymmetric(receivedBytes, Constants.getKeyForExchangeKeys());

                                                        Log.i("decrypted msg step 3:", decryptedMessage);

                                                        byte[] decryptedBytes = Hex.decodeHex(decryptedMessage.toCharArray());
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
                                                            errorReason = "Step 3: Public key sent does not correspond to the receivers public key";
                                                            sessionErrorKey = true;
                                                        }

                                                    }

                                                }
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                }

                            }

                            if (protocolId.compareTo("E") == 0) {
                                    //run protocol from the message exchange encryption
                                    } else {
                                    if (protocolId.compareTo("W") == 0) {
                                        //received the initial W
                                        Constants.setW(receivedMessage);
                                    }
                                }

                            }

                            if (sessionErrorKey) {
                                Toast.makeText(context, errorReason, Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                }
            }
        }

    public String encryptSymmetric(byte[] message, byte[] key) {

        byte[] encrypted = null;

        try{
            SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
            Constants.setLongTermSharedKeySecret(secretKeySpec);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            encrypted = cipher.doFinal(message);

            String strEncrypted = new String(Hex.encodeHex(encrypted));

            Log.i("Step 1: After enc:", strEncrypted);

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
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptSymmetric(byte[] message, byte[] key)  {

        byte[] clearText = null;

        try {
            String strEncrypted = new String(Hex.encodeHex(message));
            Log.i("DEC:B4 DEC:", strEncrypted);

            SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
            Constants.setLongTermSharedKeySecret(secretKeySpec);

            byte[] keyBytes = secretKeySpec.getEncoded();
            String strKeyBytes = new String(Hex.encodeHex(keyBytes));

            Log.i("DEC: KEY BYTES STR:", strKeyBytes);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            clearText = cipher.doFinal(message);

            return new String(Hex.encodeHex(clearText));
        }
        catch( Exception e){
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

    private static byte[] base64Decode(String s) {
        return Base64.decodeBase64(s);
    }

    private String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes).replaceAll("\\s", "");

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
