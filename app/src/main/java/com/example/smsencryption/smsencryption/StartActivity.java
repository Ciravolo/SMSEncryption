package com.example.smsencryption.smsencryption;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    Button btnSendNonce;
    Button btnStep1;
    Button btnStep2;
    Button btnSendMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSendNonce = (Button) findViewById(R.id.btnSendNonce);
        btnStep1 = (Button) findViewById(R.id.btnStep1);
        btnStep2 = (Button) findViewById(R.id.btnStep2);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);

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

    }

}
