package com.example.smsencryption.smsencryption;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SendMessageActivity extends AppCompatActivity {

    Button btnSendMessage;
    EditText txtPhoneNumber;
    EditText txtMessage;

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    String AUTH = "my.action.string";

    private BroadcastReceiver sendBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    Toast.makeText(getBaseContext(), "SMS Sent", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private BroadcastReceiver deliveryBroadcastReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context arg0, Intent arg1)
        {
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    Toast.makeText(getBaseContext(), "SMS Delivered", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private BroadcastReceiver sendStepBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(getBaseContext(), "First step: SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(getBaseContext(), "First step: SMS not delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);

        btnSendMessage.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //perform the action on click
                String phoneNumber = txtPhoneNumber.getText().toString();
                String message = txtMessage.getText().toString();

                if (phoneNumber.length()>0 && message.length()>0){

                    String sa = Constants.getSessionKeyA();
                    String sb = Constants.getSessionKeyB();

                    if ((sa.compareTo("")!=0)||(sb.compareTo("")!=0)) {

                        String sessionKeyForEncryption = (Constants.getSessionKeyA().compareTo("")==0)?Constants.getSessionKeyB():Constants.getSessionKeyA();
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
            catch(UnsupportedEncodingException e){
                e.printStackTrace();
                return null;
            }

        }

    private void sendEncryptedSMS(String phoneNumber, String message){

        String step = "3";
        Intent intent = new Intent("my.action.string");
        intent.putExtra("step_number", step);
        sendBroadcast(intent);

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

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

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

}
