package com.dotmav.smsterminal;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PreferencesActivity extends PreferenceActivity{

    private ArrayList<ContactData> contacts;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.activity_preferences);

        getContacts();

        Collections.sort(contacts, new Comparator<ContactData>() {
            @Override
            public int compare(ContactData c1, ContactData c2) {
                return c1.getFullName().compareTo(c2.getFullName());
            }
        });

        MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) findPreference("whitelist");
        ArrayList<String> listEntries = new ArrayList<String>();
        ArrayList<String> listValues = new ArrayList<String>();

        for(int i = 0; i < contacts.size(); i++){
            listEntries.add(contacts.get(i).getFullName());
            listValues.add(contacts.get(i).getNumber());
        }

        multiSelectListPreference.setEntries(listEntries.toArray(new CharSequence[listEntries.size()]));
        multiSelectListPreference.setEntryValues(listValues.toArray(new CharSequence[listValues.size()]));

    }

    private void getContacts(){
        contacts = new ArrayList<ContactData>();
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);       //HELP
        cursor.moveToFirst();
        while(cursor.moveToNext()){
            ContactData new_contact = new ContactData(
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            );
            contacts.add(new_contact);
        }
    }
}
