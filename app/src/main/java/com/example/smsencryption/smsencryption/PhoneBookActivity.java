package com.example.smsencryption.smsencryption;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

import com.example.smsencryption.smsencryption.database.SMSEncryptionContract;
import com.example.smsencryption.smsencryption.database.SMSEncryptionDbHelper;
import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;

import org.apache.commons.codec.binary.Hex;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhoneBookActivity extends AppCompatActivity {

    private ListView list;
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

        //TODO: check on the database if the keys are already set, if not then create new ones

        TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String myPhoneNumber = tMgr.getLine1Number();

        Log.i("my phone number is:", myPhoneNumber);

        if ((myPhoneNumber.compareTo("")!=0)&&(!myPhoneNumber.contains("?"))){

            SMSEncryptionDbHelper mDbHelper = new SMSEncryptionDbHelper(getBaseContext());

            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            String[] projection = {
                    SMSEncryptionContract.Directory._ID,
                    SMSEncryptionContract.Directory.COLUMN_NAME_PHONENUMBER,
                    SMSEncryptionContract.Directory.COLUMN_NAME_PUBLICKEY,
                    SMSEncryptionContract.Directory.COLUMN_NAME_PRIVATEKEY
            };

            // Filter results WHERE "title" = 'My Title'
            String selection = SMSEncryptionContract.Directory.COLUMN_NAME_PHONENUMBER + " = ?";
            String[] selectionArgs = { myPhoneNumber };

            Cursor cursor = db.query(
                    SMSEncryptionContract.Directory.TABLE_NAME,// The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                      // The sort order
            );

            List itemIds = new ArrayList<>();
            while(cursor.moveToNext()) {
                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(SMSEncryptionContract.Directory._ID));
                itemIds.add(itemId);
            }
            cursor.close();

            SQLiteDatabase dbw = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            if (itemIds.size()==0) {
            //Keys do not exist so we need to create a pair and save it on the database
                Utils u = new Utils();
                //I get a pair of keys for RSA to set my public key/private key
                Map<String, Object> keys = null;
                try {
                    keys = u.getRSAKeys();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Generation of public and private keys on Bob
                PrivateKey privateKey = (PrivateKey) keys.get("private");
                PublicKey publicKey = (PublicKey) keys.get("public");

                Constants.setMyPrivateKey(privateKey);
                Constants.setMyPublicKey(publicKey);

                byte[] bytesMyPublicKey = Constants.getMyPublicKey().getEncoded();
                String strMyPublicKey = new String(Hex.encodeHex(bytesMyPublicKey));

                byte[] bytesMyPrivateKey = Constants.getMyPrivateKey().getEncoded();
                String strMyPrivateKey = new String(Hex.encodeHex(bytesMyPrivateKey));

                //TODO: record on the database my own public key

                //insert the 2 strings with my phone number on it on the DB

                Log.i("I:", "it is not saved in the database, first time to record my public and private keys.");
                //TODO: It is not saved in the db so save the key of Bob
                //save values on the database

                values.put(SMSEncryptionContract.Directory.COLUMN_NAME_PHONENUMBER, myPhoneNumber);
                values.put(SMSEncryptionContract.Directory.COLUMN_NAME_PUBLICKEY, strMyPublicKey);
                values.put(SMSEncryptionContract.Directory.COLUMN_NAME_PRIVATEKEY, strMyPrivateKey);

                //Insert the row
                long newRowId = dbw.insert(SMSEncryptionContract.Directory.TABLE_NAME, null, values);

                Log.i("I:", "Inserted row with id:"+newRowId);

            }
            else{

                String[] projection2 = {
                        SMSEncryptionContract.Directory._ID,
                        SMSEncryptionContract.Directory.COLUMN_NAME_PHONENUMBER,
                        SMSEncryptionContract.Directory.COLUMN_NAME_PUBLICKEY,
                        SMSEncryptionContract.Directory.COLUMN_NAME_PRIVATEKEY
                };

                // Filter results WHERE "title" = 'My Title'
                String selection2 = SMSEncryptionContract.Directory.COLUMN_NAME_PHONENUMBER + " = ?";
                String[] selectionArgs2 = { myPhoneNumber };

                Cursor cursor2 = db.query(
                        SMSEncryptionContract.Directory.TABLE_NAME,// The table to query
                        projection,                               // The columns to return
                        selection,                                // The columns for the WHERE clause
                        selectionArgs,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        null                                      // The sort order
                );

                List itemPubKey = new ArrayList<>();
                List itemPrivKey = new ArrayList<>();

                while(cursor2.moveToNext()) {
                    String pubKey = cursor2.getString(cursor.getColumnIndex(SMSEncryptionContract.Directory.COLUMN_NAME_PUBLICKEY));
                    String privKey = cursor2.getString(cursor.getColumnIndex(SMSEncryptionContract.Directory.COLUMN_NAME_PRIVATEKEY));
                    itemPubKey.add(pubKey);
                    itemPrivKey.add(privKey);
                }
                cursor.close();

                //as each of the lists have just one single Item for each user (public and private that was saved before)
                //I obtain it from there and set them

            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
