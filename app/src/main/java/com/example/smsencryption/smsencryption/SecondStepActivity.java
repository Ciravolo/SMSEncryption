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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


/**
 * Reply from user to obtain the session key
 */
public class SecondStepActivity extends AppCompatActivity {

    EditText txtPhoneNumber;
    Button btnSendSecondStep;

    private BroadcastReceiver  sendBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
    };

    private BroadcastReceiver deliveryBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                    Toast.makeText(getBaseContext(), "Second step: SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(getBaseContext(), "Second step: SMS not delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    String STEP = "my.action.string";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_second_step);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtPhoneNumber = (EditText) findViewById(R.id.txtPhoneNumber);
        btnSendSecondStep = (Button) findViewById(R.id.btnSendSecondStep);
        btnSendSecondStep.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                String phoneNumber = txtPhoneNumber.getText().toString();

                if (phoneNumber.length()>0){
                        String encryptedMessage = generateEncryptedDataSecondStep();
                        sendEncryptedSMS(phoneNumber, encryptedMessage);
                } else{

                    new AlertDialog.Builder(SecondStepActivity.this)
                            .setTitle("Alert")
                            .setMessage("Please insert a valid phone number.")
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

    public String generateEncryptedDataSecondStep(){
        try{

            String sa = Constants.getPrivateKeyA();
            String sb = Constants.getPrivateKeyB();

            String pa = Constants.getPinA();
            String pb = Constants.getPinB();

            String saa = sa;
            String sbb = sb;

            String ppa = pa;
            String ppb = pb;

            //TODO: change to Base64 decoding
            byte[] stringToEncrypt = (Constants.getPrivateKeyA() + Constants.getPinB() + Constants.getPinA()).getBytes("UTF-8");
            byte[] longTermSharedKeyString = (Constants.getLongTermSharedKey()).getBytes("UTF-8");

            try{
                MessageDigest sha = MessageDigest.getInstance("SHA-1");
                longTermSharedKeyString = sha.digest(longTermSharedKeyString);
                longTermSharedKeyString = Arrays.copyOf(longTermSharedKeyString, 16); // use only first 128 bit

                SecretKeySpec secretKeySpec = new SecretKeySpec(longTermSharedKeyString, "AES");
                Constants.setLongTermSharedKeySecret(secretKeySpec);

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

    private void sendEncryptedSMS(String phoneNumber, String encryptedMessage){

        /*String step = "2";
        Intent intent = new Intent("my.action.string");
        intent.putExtra("step_number", step);
        sendBroadcast(intent);
*/
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

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
    protected void onStart() {
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
    public void onPause(){
        super.onPause();
    }

}
