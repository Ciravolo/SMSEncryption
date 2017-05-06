package com.example.smsencryption.smsencryption;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    Button btnStartProtocol;
    Button btnIHaveAKey;
    Button btnSendMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnStartProtocol = (Button) findViewById(R.id.btnStartProtocol);
        btnIHaveAKey = (Button) findViewById(R.id.btnIHaveAKey);
        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);

        btnStartProtocol.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol with user 1
                Intent intentProtocol = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intentProtocol);
            }
        });

        btnIHaveAKey.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //start the protocol from user 2, the one who receives the key
                Intent intentKey = new Intent(StartActivity.this, Main2Activity.class);
                startActivity(intentKey);
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
