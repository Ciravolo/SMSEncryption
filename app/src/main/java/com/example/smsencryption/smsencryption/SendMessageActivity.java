package com.example.smsencryption.smsencryption;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class SendMessageActivity extends AppCompatActivity {

    Button btnSend;
    EditText txtPhoneNumber;
    EditText txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnSend = (Button) findViewById(R.id.btnStart);

        btnSend.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //perform the action on click
                String phoneNumber = txtPhoneNumber.getText().toString();
                String message = txtMessage.getText().toString();

                if (phoneNumber.length()>0 && message.length()>0){

                    if ((Constants.SESSION_KEY_A.compareTo("")!=0)||(Constants.SESSION_KEY_B.compareTo("")!=0)) {

                        String sessionKeyForEncryption = (Constants.SESSION_KEY_A.compareTo("")==0)?Constants.SESSION_KEY_B:Constants.SESSION_KEY_A;
                        String encryptedMessage = encryptWithSessionKey(sessionKeyForEncryption, message);
                        sendEncryptedSMS(phoneNumber, encryptedMessage);
                    }
                    else{
                        new AlertDialog.Builder(SendMessageActivity.this)
                                .setTitle("Alert")
                                .setMessage("A session key could not be established, please start the protocol.")
                                .setCancelable(false)
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //close the dialog
                                        dialog.cancel();
                                    }
                                }).show();
                    }

                } else{
                    new AlertDialog.Builder(SendMessageActivity.this)
                            .setTitle("Alert")
                            .setMessage("Please fill all the fields before sending the message.")
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


    public String encryptWithSessionKey(String sessionKey, String message) {

            try{
                byte[] stringToEncrypt = message.getBytes("UTF-8");
                byte[] sessionKeyBytes = (sessionKey).getBytes("UTF-8");

                try{
                    MessageDigest sha = MessageDigest.getInstance("SHA-1");
                    sessionKeyBytes = sha.digest(sessionKeyBytes);
                    sessionKeyBytes = Arrays.copyOf(sessionKeyBytes, 16); // use only first 128 bit

                    SecretKeySpec secretKeySpec = new SecretKeySpec(sessionKeyBytes, "AES");

                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                    byte[] encrypted = cipher.doFinal(stringToEncrypt);

                    String strEncrypted = new String(encrypted, "UTF-8");
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
            catch(UnsupportedEncodingException e){
                e.printStackTrace();
                return null;
            }

        }

    private void sendEncryptedSMS(String phoneNumber, String message){

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        Intent intentSent = new Intent(SENT);
        intentSent.putExtra("message_sent", "1");

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                intentSent, 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        try{
            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
