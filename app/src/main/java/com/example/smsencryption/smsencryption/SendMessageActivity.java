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
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SendMessageActivity extends AppCompatActivity {

    Button btnStart;
    Button btnGetNonce;

    TextView lblTime;
    TextView lblNonce;
    TextView lblPKGenerated;

    EditText txtNonce;
    EditText txtPhoneNumber;
    EditText txtSharedKey;
    EditText txtMessage;

    private boolean isTimerEnabled = false;

    byte[] arrNonce;
    private String stringNonce;

    String stringPK;
    String string_nonce;

    private String publicKey;
    private String nonce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        txtNonce = (EditText) findViewById(R.id.txtNonce);
        txtSharedKey = (EditText) findViewById(R.id.txtSharedKey);
        txtMessage = (EditText) findViewById(R.id.txtMessage);

        lblTime = (TextView) findViewById(R.id.lblTime);
        lblNonce = (TextView) findViewById(R.id.lblNonce);
        lblPKGenerated = (TextView) findViewById(R.id.lblPKGenerated);

        btnStart = (Button) findViewById(R.id.btnStart);

        btnStart.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //perform the action on click
                String phoneNumber = txtPhoneNumber.getText().toString();
                nonce = txtNonce.getText().toString();
                publicKey = txtSharedKey.getText().toString();
                String message = txtMessage.getText().toString();

                if (phoneNumber.length()>0 && nonce.length()>0 && publicKey.length()>0 && message.length()>0){

                    if (nonce.compareTo(stringNonce)==0){
                        //if the nonce matches then encrypt the message and send it
                        /*short shortPK = Short.parseShort(publicKey);
                        int intPK = (int) shortPK;
                        byte[] encodedPK = ByteBuffer.allocate(16).putInt(intPK).array();
                        try {
                            stringPK = new String(encodedPK, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }*/

                        /*short short_nonce = Short.parseShort(stringNonce);
                        int intNonce = (int) short_nonce;

                        byte[] encodedNonce = ByteBuffer.allocate(16).putInt(intNonce).array();
                        try {
                            string_nonce = new String(encodedNonce, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }*/

                        String encryptedMessage = encrypt(publicKey, nonce, message);
                        sendEncryptedSMS(phoneNumber, encryptedMessage);
                    }
                    else{
                        //insert the appropriate nonce in the textfield
                        new AlertDialog.Builder(SendMessageActivity.this)
                                .setTitle("Alert")
                                .setMessage("Please insert the correspondent nonce generated as it is shown in the screen.")
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
                            .setMessage("Please fill all the fields before starting the protocol.")
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

        // button to obtain a new nonce
        btnGetNonce = (Button) findViewById(R.id.btnGetNonce);
        btnGetNonce.setOnClickListener(new View.OnClickListener(){

            //start the timer for the nonce
            public void onClick(View v){

                if (!isTimerEnabled){

                    new CountDownTimer(30000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            isTimerEnabled = true;
                            lblTime.setText("Time: " + millisUntilFinished / 1000);
                            //here you can have your logic to set text to edittext
                        }

                        public void onFinish() {
                            lblTime.setText("Nonce has expired");
                            isTimerEnabled = false;
                        }

                    }.start();

                    //arrNonce = generateNonce();
                    /*Integer intNonce = java.nio.ByteBuffer.wrap(arrNonce).getInt();
                    short shortNonce = intNonce.shortValue();

                    if(shortNonce<0){
                        shortNonce = (short) (shortNonce * -1);
                    }
                    stringNonce = String.valueOf(shortNonce);
                    */
                    //stringNonce = android.util.Base64.encodeToString(arrNonce, android.util.Base64.NO_WRAP);
                    stringNonce = generateNonce();
                    lblNonce.setText("Nonce generated: "+ stringNonce);
                }

            }
        });
    }


    public static String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes());
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeBase64String(encrypted);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String generateNonce(){

        /*KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(128);
        SecretKey nonceKey = keyGen.generateKey();
        byte[] arrNonceKey = nonceKey.getEncoded();
        */

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.encodeBase64String(bytes);
    }

    private void sendEncryptedSMS(String phoneNumber, String message){

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        Intent intentSent = new Intent(SENT);
        intentSent.putExtra("DATA_ENCRYPTED", 1);
        intentSent.putExtra("SHARED_KEY", publicKey);
        intentSent.putExtra("NONCE",nonce);

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
