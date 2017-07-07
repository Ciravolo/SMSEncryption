package com.example.smsencryption.smsencryption;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class InsertQRCodeManually extends AppCompatActivity {

    private String contactName;
    private String phoneNumber;

    private Button startProtocolButton;

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_qrcode_manually);

        Bundle bundle = this.getIntent().getExtras();
        contactName = bundle.getString("NAME");
        phoneNumber = bundle.getString("PHONENUMBER");

        startProtocolButton = (Button) findViewById(R.id.btnStartProtocol);

        startProtocolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //calculate my nonce, and send it to A
                Utils u = new Utils();
                String nonce = u.generateNonce();
                Constants.setMyNonce(nonce);

                sendSMS(phoneNumber, nonce+":P:0");
            }
        });

    }


    private void sendSMS(String phoneNumber, String encryptedMessage){

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        try{
            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();

            //before sending the message I append the step
            String m = encryptedMessage;

            sms.sendTextMessage(phoneNumber, null, m, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


}
