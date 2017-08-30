package com.example.smsencryption.smsencryption;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SendMessageActivity extends AppCompatActivity {

    private String sessionKey = "";
    private String phoneNumber= "";

    private Button btnSendMessage;
    private EditText txtMessageToSend;

    private String SENT = "SMS_SENT";
    private String DELIVERED = "SMS_DELIVERED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        Bundle bundle = this.getIntent().getExtras();
        sessionKey = bundle.getString("SESSION_KEY");
        phoneNumber = bundle.getString("RECEIVER_PHONENUMBER");

        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);
        txtMessageToSend = (EditText) findViewById(R.id.txtMessageToSend);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (txtMessageToSend.getText().toString().compareTo("")==0){
                    //send an alarm saying to enter a text first
                    AlertDialog alertDialog = new AlertDialog.Builder(SendMessageActivity.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Please insert a message.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
                else{
                    //send the message encrypted with the session key to the receiver
                    if ((sessionKey.compareTo("")!=0)&&(phoneNumber.compareTo("")!=0)){
                        //if the parameters have been set correctly then send the message
                        sendEncryptedSMS(phoneNumber, sessionKey, txtMessageToSend.getText().toString());
                    }
                }


            }
        });

    }

    private void sendEncryptedSMS(String phoneNumber, String sessionKey, String plainText){

        Intent sendReceiverPhoneNumber = new Intent("sendReceiverPhone");
        sendReceiverPhoneNumber.putExtra("receiverphonenumber", phoneNumber);
        sendBroadcast(sendReceiverPhoneNumber);

        try{

            byte[] inputByte = plainText.getBytes("UTF-8");

           // byte[] plainTextBytes = Base64.encode(plainText.getBytes(), Base64.DEFAULT);
            byte[] sessionKeyBytes = Hex.decodeHex(sessionKey.toCharArray());

            String messageEncrypted = encryptSymmetrically(inputByte, sessionKeyBytes);
            messageEncrypted = messageEncrypted+":M";

            Log.i("SEND ON ENC:", messageEncrypted);

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), PendingIntent.FLAG_ONE_SHOT);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), PendingIntent.FLAG_ONE_SHOT);

            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();

            sms.sendTextMessage(phoneNumber, null, messageEncrypted, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    public String encryptSymmetrically(byte[] message, byte[] key) {

        byte[] encrypted = null;

        try{
            SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encrypted = cipher.doFinal(message);
            String strEncrypted = new String(Base64.encode(encrypted, Base64.DEFAULT));
            Log.i("Step 1: After enc:", strEncrypted);
            return strEncrypted;

        } catch(NoSuchAlgorithmException e){
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

}
