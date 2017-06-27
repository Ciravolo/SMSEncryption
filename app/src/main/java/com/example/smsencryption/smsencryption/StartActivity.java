package com.example.smsencryption.smsencryption;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class StartActivity extends AppCompatActivity {

    Button btnSendNonce;
    Button btnStep1;
    Button btnStep2;
    Button btnSendMessage;
    Button btnGenerateQRCode;
    Button btnScanQRCode;
    Button btnKeysExchange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},1);

        btnSendNonce = (Button) findViewById(R.id.btnSendNonce);
        btnStep1 = (Button) findViewById(R.id.btnStep1);
        btnStep2 = (Button) findViewById(R.id.btnStep2);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);
        btnKeysExchange = (Button) findViewById(R.id.btnKeysExchange);

        btnGenerateQRCode = (Button) findViewById(R.id.btnGenerateQRCode);

        btnScanQRCode = (Button) findViewById(R.id.btnScanQRCode);

        btnGenerateQRCode.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol with user 1
                Intent intentStart = new Intent(StartActivity.this, GenerateQRCodeActivity.class);
                startActivity(intentStart);
            }
        });

        btnKeysExchange.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol with user 1
                Intent intentKeyExchange = new Intent(StartActivity.this, KeysExchangeActivity.class);
                startActivity(intentKeyExchange);
            }
        });

        btnSendNonce.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol with user 1
                Intent intentStart = new Intent(StartActivity.this, SendNonceActivity.class);
                startActivity(intentStart);
            }
        });

        btnStep1.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol from user 2, the one who receives the key
                Intent intentFirstStep = new Intent(StartActivity.this, FirstStepActivity.class);
                startActivity(intentFirstStep);
            }

        });

        btnStep2.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start sending the message here
                Intent intentSecondStep = new Intent(StartActivity.this, SecondStepActivity.class);
                startActivity(intentSecondStep);
            }

        });

        btnSendMessage.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start sending the message here
                Intent intentSendMessage = new Intent(StartActivity.this, SendMessageActivity.class);
                startActivity(intentSendMessage);
            }

        });

        btnScanQRCode.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start sending the message here
                //Intent intentScanQRCode = new Intent(StartActivity.this, ScanQRCode.class);
                //startActivity(intentScanQRCode);
                IntentIntegrator integrator = new IntentIntegrator(StartActivity.this);
                integrator.initiateScan();

            }

        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            // handle scan result
            String content = scanResult.getContents();
            Log.i("content obtained:", content);
            //TODO: Obtener la cadena del qrcode y pasarlo a byte[] para setearlo luego
            Constants.setW(content);
        }
        // else continue with any other code you need in the method

    }

}
