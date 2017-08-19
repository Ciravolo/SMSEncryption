package com.example.smsencryption.smsencryption.database;

import android.provider.BaseColumns;

import java.security.PublicKey;

/**
 * Created by joana on 8/19/17.
 */

public final class SMSEncryptionContract {

    private SMSEncryptionContract(){}

    public static class Directory implements BaseColumns{
        public static final String TABLE_NAME="directory";
        public static final String COLUMN_NAME_PHONENUMBER = "phonenumber";
        public static final String COLUMN_NAME_PUBLICKEY = "publickey";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Directory.TABLE_NAME + " (" +
                    Directory._ID + " INTEGER PRIMARY KEY," +
                    Directory.COLUMN_NAME_PHONENUMBER + " TEXT," +
                    Directory.COLUMN_NAME_PUBLICKEY + " TEXT)";


    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Directory.TABLE_NAME;

}
