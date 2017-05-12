package com.example.smsencryption.smsencryption;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class SecondStepActivity extends AppCompatActivity {

    EditText txtPhoneNumber;
    Button btnSendSecondStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

            byte[] stringToEncrypt = (Constants.PRIVATE_KEY_A + Constants.PIN_B + Constants.PIN_A).getBytes("UTF-8");
            byte[] longTermSharedKeyString = (Constants.LONGTERM_SHARED_KEY).getBytes("UTF-8");

            try{
                MessageDigest sha = MessageDigest.getInstance("SHA-1");
                longTermSharedKeyString = sha.digest(longTermSharedKeyString);
                longTermSharedKeyString = Arrays.copyOf(longTermSharedKeyString, 16); // use only first 128 bit

                SecretKeySpec secretKeySpec = new SecretKeySpec(longTermSharedKeyString, "AES");
                Constants.LONGTERM_SHARED_KEY_SECRET = secretKeySpec;

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                byte[] encrypted = cipher.doFinal(stringToEncrypt);

                //String strEncrypted = Base64.encodeBase64String(encrypted);
                String strEncrypted = new String(encrypted, "UTF-8");
                //append nb = myNonce
                //String finalString = Constants.PIN_A + strEncrypted;
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

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        Intent intentSent = new Intent(SENT);
        intentSent.putExtra("SECOND_STEP_SESSION_KEY", "1");

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
            sms.sendTextMessage(phoneNumber, null, encryptedMessage, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


}
