package com.example.smsencryption.smsencryption;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;

import java.util.ArrayList;
import java.util.List;

public class PhoneBookActivity extends AppCompatActivity {

    private ListView list;
    private Cursor cursor1;
    private FloatingActionButton fabAdd;
    private FABToolbarLayout morph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_book);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        morph = (FABToolbarLayout) findViewById(R.id.fabtoolbar);

        View optionAddContact, optionInfo, optionUpdateKeys;

        optionAddContact = findViewById(R.id.optionAddContact);
        optionInfo = findViewById(R.id.optionInfo);
        optionUpdateKeys = findViewById(R.id.optionUpdateKeys);

        list = (ListView)findViewById(R.id.listContacts);

        List<PhoneBook> listPhoneBook = new ArrayList<PhoneBook>();
        listPhoneBook.add(new PhoneBook("Contact 1", "5555"));
        listPhoneBook.add(new PhoneBook("Contact 1", "5555"));
        listPhoneBook.add(new PhoneBook("Contact 1", "5555"));

        PhoneBookAdapter adapter = new PhoneBookAdapter(this, listPhoneBook);
        list.setAdapter(adapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                morph.show();
            }
        });

        optionInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        optionAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start the protocol with user 1
                Intent intentStart = new Intent(PhoneBookActivity.this, AddContact.class);
                startActivity(intentStart);
            }
        });

        optionUpdateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
