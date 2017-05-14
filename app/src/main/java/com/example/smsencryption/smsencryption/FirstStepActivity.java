package com.example.smsencryption.smsencryption;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 *  Class related to the first step of encryption: reply to user after having received the nonce
 */
public class FirstStepActivity extends AppCompatActivity {

    Button btnSendFirstStep;
    Button btnGetNonceFirstStep;
    TextView lblTime;
    TextView lblNonce;
    EditText txtNonce;
    EditText txtPhoneNumber;
    EditText txtNonceReceived;

    private boolean isTimerEnabled = false;
    private String stringNonce;
    private String nonce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_first_step);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        txtNonce = (EditText) findViewById(R.id.txtNonce);
        txtNonceReceived = (EditText) findViewById(R.id.txtNonceReceived);

        lblTime = (TextView) findViewById(R.id.lblTime);
        lblNonce = (TextView) findViewById(R.id.lblNonce);

        btnSendFirstStep = (Button) findViewById(R.id.btnSendFirstStep);
        btnGetNonceFirstStep = (Button) findViewById(R.id.btnGetNonceFirstStep);

        btnSendFirstStep.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                String phoneNumber = txtPhoneNumber.getText().toString();
                nonce = txtNonce.getText().toString();
                String nonceReceived = txtNonceReceived.getText().toString();

                if (phoneNumber.length()>0 && nonce.length()>0 && nonceReceived.length()>0){

                    if (nonce.compareTo(stringNonce)==0){
                        Constants.setPinB(nonce);
                        String encryptedMessage = generateEncryptedData(nonceReceived);
                        sendEncryptedSMS(phoneNumber, encryptedMessage);
                    }
                    else{

                        new AlertDialog.Builder(FirstStepActivity.this)
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

                    new AlertDialog.Builder(FirstStepActivity.this)
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

        btnGetNonceFirstStep.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                if (!isTimerEnabled){

                    new CountDownTimer(30000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            isTimerEnabled = true;
                            lblTime.setText("Time: " + millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            lblTime.setText("Nonce has expired");
                            isTimerEnabled = false;
                        }

                    }.start();

                    stringNonce = generateNonce();
                    lblNonce.setText("Nonce generated: "+ stringNonce);
                }

            }
        });
    }


    public String generateEncryptedData(String nonceReceived) {

        try{

            Constants.setPinA(nonceReceived);


            //TODO: change to base64 decoding
            //byte[] stringToEncrypt = Base64.encodeBase64((Constants.getPrivateKeyB() + Constants.getPinA() + Constants.getPinB()).getBytes());
            //byte[] longTermSharedKeyString = Base64.encodeBase64(Constants.getLongTermSharedKey().getBytes());

            byte[] stringToEncrypt = (Constants.getPrivateKeyB() + Constants.getPinA() + Constants.getPinB()).getBytes("UTF-8");
            byte[] longTermSharedKeyString = Constants.getLongTermSharedKey().getBytes("UTF-8");

                MessageDigest sha = MessageDigest.getInstance("SHA-1");
                longTermSharedKeyString = sha.digest(longTermSharedKeyString);
                longTermSharedKeyString = Arrays.copyOf(longTermSharedKeyString, 16); // use only first 128 bit

                SecretKeySpec secretKeySpec = new SecretKeySpec(longTermSharedKeyString, "AES");
                Constants.setLongTermSharedKeySecret(secretKeySpec);

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                byte[] encrypted = cipher.doFinal(stringToEncrypt);

                //byte[] valueDecoded = Base64.decodeBase64(encrypted);
                //String strEncrypted = new String(valueDecoded);

                String strEncrypted = new String(Base64.encodeBase64(encrypted));
                strEncrypted.replace('+','-').replace('/','_');

                String finalString = Constants.getPinB() + strEncrypted;
                return finalString;
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
            } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
                return null;
            }



    }

    private String generateNonce(){
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String nonceGenerated = new String(Base64.encodeBase64(bytes));
        return  nonceGenerated.replace('+','-').replace('/','_');
    }

    private void sendEncryptedSMS(String phoneNumber, String encryptedMessage){

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        String step = "1";

        Intent intent = new Intent("my.action.string");
        intent.putExtra("step_number", step);
        sendBroadcast(intent);

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


        //row row fight the power!
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
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
        }, new IntentFilter("my.action.string"));

        try{
            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();
            sms.sendTextMessage(phoneNumber, null, encryptedMessage, sentPI, deliveredPI);
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
