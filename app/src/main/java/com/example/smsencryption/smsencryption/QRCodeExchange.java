package com.example.smsencryption.smsencryption;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRCodeExchange extends AppCompatActivity {

    private View btnGenerateQRCode;
    private View btnScanQRCode;
    private View btnInsertQRCode;

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    String contactName;
    String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_exchange);

        Bundle bundle = this.getIntent().getExtras();
        contactName = bundle.getString("NAME");
        phoneNumber = bundle.getString("PHONENUMBER");

        btnGenerateQRCode = (View) findViewById(R.id.btnGenerateQRCode);
        btnScanQRCode = (View) findViewById(R.id.btnScanQRCode);
        btnInsertQRCode = (View) findViewById(R.id.btnInsertQRCode);

        btnGenerateQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentStart = new Intent(QRCodeExchange.this, GenerateQRCodeActivity.class);
                intentStart.putExtra("NAME", contactName);
                intentStart.putExtra("PHONENUMBER", phoneNumber);
                startActivity(intentStart);
            }
        });

        btnScanQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IntentIntegrator integrator = new IntentIntegrator(QRCodeExchange.this);
                integrator.initiateScan();
            }
        });

        btnInsertQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentStart = new Intent(QRCodeExchange.this, InsertQRCodeManually.class);
                intentStart.putExtra("NAME", contactName);
                intentStart.putExtra("PHONENUMBER", phoneNumber);
                startActivity(intentStart);
            }
        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            // handle scan result
            String content = scanResult.getContents();
            Log.i("content obtained:", content);
            Constants.setW(content);
            //I scan the qr code and then I pass my nonce to B
            if (content!=null){
                Utils u = new Utils();
                String nonce = u.generateNonce();
                Constants.setMyNonce(nonce);
                //Send my nonce to Bob
                sendNonce(phoneNumber, Constants.getMyNonce());
            }

        }
        // else continue with any other code you need in the method

    }

    private void sendNonce(String phoneNumber, String nonce){

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        try{
            SmsManager sms = SmsManager.getDefault();
            Toast.makeText(getApplicationContext(), "Phone number to send:"+phoneNumber, Toast.LENGTH_LONG).show();

            //before sending the message I append the id of the protocol
            String m = nonce+":P:0";

            sms.sendTextMessage(phoneNumber, null, m, sentPI, deliveredPI);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed, please try again later", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}


