package com.example.smsencryption.smsencryption;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class KeysExchangeActivity extends AppCompatActivity {

    EditText txtPhoneNumber;
    Button btnSendKeys;
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keys_exchange);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        btnSendKeys = (Button) findViewById(R.id.btnSendKeys);

        btnSendKeys.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                String phoneNumber = txtPhoneNumber.getText().toString();

                if (phoneNumber.length()>0 ) {
                    try {
                        byte[] keyForExchangeKeys = generateKeyHKDF("", Constants.getW());

                        sendPublicKey(phoneNumber, keyForExchangeKeys);

                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                else{
                    new AlertDialog.Builder(KeysExchangeActivity.this)
                            .setTitle("Alert")
                            .setMessage("Please insert a phone number.")
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //close the dialog
                                    dialog.cancel();
                                }
                            }).show();
                }


            }
        });
    }

    public void sendPublicKey(String phoneNumber, byte[] keyForEncryption) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockingModeException,
            IllegalBlockSizeException{

        //TODO: Change this string for a string containing the public key to be sent
        byte[] stringToEncrypt = ("blablabla").getBytes("UTF-8");

        SecretKeySpec secretKeySpec = new SecretKeySpec(keyForEncryption, "AES");
        Constants.setLongTermSharedKeySecret(secretKeySpec);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encrypted = cipher.doFinal(stringToEncrypt);

        String strEncrypted = new String(Base64.encodeBase64(encrypted));
        strEncrypted.replace('+', '-').replace('/', '_');

        //now send the text

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        try{
            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();

            //before sending the message I append the step
            String m = strEncrypted+":1";

            sms.sendTextMessage(phoneNumber, null, m, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    public static byte[] generateKeyHKDF(String salt, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return mac.doFinal(salt.getBytes());
    }

}
