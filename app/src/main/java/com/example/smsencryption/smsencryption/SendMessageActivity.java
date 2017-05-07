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
import android.provider.Settings;
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
import android.os.Handler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.SecureRandom;
import java.util.Formatter;

public class SendMessageActivity extends AppCompatActivity {

    Button btnStart;
    Button btnGetNonce;

    TextView lblTime;
    TextView lblNonce;
    TextView lblPKGenerated;

    EditText txtNonce;
    EditText txtPhoneNumber;

    private boolean isTimerEnabled = false;
    private boolean publicKeyNoGenerated = false;

    private String nonceText;

    private byte[] arrPublicKey;
    private SecretKey publicKey;
    private short shortPublicKey;

    private byte[] arrNonce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        txtNonce = (EditText) findViewById(R.id.txtNonce);

        lblTime = (TextView) findViewById(R.id.lblTime);
        lblNonce = (TextView) findViewById(R.id.lblNonce);
        lblPKGenerated = (TextView) findViewById(R.id.lblPKGenerated);

        btnStart = (Button) findViewById(R.id.btnStart);

        //Generate the public key
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            publicKey = keyGen.generateKey();
            arrPublicKey = publicKey.getEncoded();

            Integer intPKey = java.nio.ByteBuffer.wrap(arrPublicKey).getInt();
            shortPublicKey = intPKey.shortValue();

            if (shortPublicKey<0){
                shortPublicKey = (short) (shortPublicKey * -1);
            }
            lblPKGenerated.setText("Public Key generated: "+shortPublicKey);
        }
        catch(NoSuchAlgorithmException e){

            publicKeyNoGenerated = true;
            Toast.makeText(getBaseContext(), "Could not generate public key",
                    Toast.LENGTH_SHORT).show();
        }

        btnStart.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //perform the action on click
                String phoneNumber = txtPhoneNumber.getText().toString();
                String nonce = txtNonce.getText().toString();

                if (phoneNumber.length()>0 && nonce.length()>0){
                    //if the fields are full then send the message = hash (xor(nonce and public key))

                    //first xor the array of bytes
                    if (nonce.compareTo(nonceText)==0){

                        //first : xor(nonce + pkey)
                        if (!publicKeyNoGenerated){
                            //means that the public key was correctly generated, then xor both pk and nonce
                            byte[] xor_nonce_pk_array = xor_arrays(arrNonce, arrPublicKey);



                        }
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

                    arrNonce = generateNonce();
                    Integer intNonce = java.nio.ByteBuffer.wrap(arrNonce).getInt();
                    short shortNonce = intNonce.shortValue();

                    if(shortNonce<0){
                        shortNonce = (short) (shortNonce * -1);
                    }
                    lblNonce.setText("Nonce generated: "+ shortNonce);
                    nonceText = Short.toString(shortNonce);

                }

            }
        });
    }


    public static String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            System.out.println("encrypted string: "
                    + Base64.encodeBase64String(encrypted));

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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

    private static String byteArray2Hex(final byte[] hash){

        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    /**
     * Xor arrays from 2 array of bytes of the same size, by default obtaining the length for the
     * result from the first array
     * @param array1
     * @param array2
     * @return
     */
    private byte[] xor_arrays(byte[] array1, byte[] array2){

        int length1 = array1.length;
        byte[] result = new byte[length1];

        int i =0;

        for (byte b : array1){
            result[i] = (byte)(b ^ array2[i++]);
        }

        return result;
    }

    private byte[] generateNonce(){
        try{
            SecureRandom random = new SecureRandom();
            byte[] values = new byte[20];
            random.nextBytes(values);
            return values;
        }
        catch(Exception e){
            //case in which the nonce could not be created
            Toast.makeText(getBaseContext(), "Could not create nonce",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void sendSMS(String phoneNumber, String message){

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

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
