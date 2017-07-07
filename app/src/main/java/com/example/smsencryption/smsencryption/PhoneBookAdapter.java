package com.example.smsencryption.smsencryption;


import android.content.Context;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class PhoneBookAdapter extends BaseAdapter{

    private Context mContext;
    private List<PhoneBook> mListPhoneBook;

    public PhoneBookAdapter(Context context, List<PhoneBook> list){
        mContext = context;
        mListPhoneBook = list;
    }

    @Override
    public int getCount() {
        return mListPhoneBook.size();
    }

    @Override
    public Object getItem(int position) {
        return mListPhoneBook.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PhoneBook entry = mListPhoneBook.get(position);

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.phonebook_row, null);

        }

        TextView tvName = (TextView) convertView.findViewById(R.id.txtContactName);
        tvName.setText(entry.getmName());

        TextView tvPhoneNUmber = (TextView) convertView.findViewById(R.id.txtPhoneNumber);
        tvPhoneNUmber.setText(entry.getmPhone());

        return convertView;
    }
}
