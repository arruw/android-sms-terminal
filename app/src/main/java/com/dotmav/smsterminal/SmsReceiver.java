package com.dotmav.smsterminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver{

    private boolean enable_whitelist = false;
    private String[] whitelist = null;

    private boolean secured = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        final Bundle bundle = intent.getExtras();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        enable_whitelist = sharedPreferences.getBoolean("enable_whitelist", false);
        if(enable_whitelist) whitelist = sharedPreferences.getStringSet("whitelist", null).toArray(new String[]{});

        try{
            if(bundle != null){
                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for(int i = 0; i < pdusObj.length; i++){
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String senderNum = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();

                    if(enable_whitelist){
                        if(whitelistContains(senderNum)){
                            secured = true;
                        } else {
                            secured = false;
                        }
                    } else {
                        secured = true;
                    }

                    if(secured == true && message.split(" ")[0].equals("smsts")) {

                        Intent terminalServiceIntent = new Intent(context, TerminalService.class);
                        terminalServiceIntent.putExtra("command", message);
                        terminalServiceIntent.putExtra("number", senderNum);
                        terminalServiceIntent.putExtra("secured", secured);
                        context.startService(terminalServiceIntent);

                    }

                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private boolean whitelistContains(String data){
        if(whitelist == null) return false;
        for(int i = 0; i < whitelist.length; i++){
            if((whitelist[i].substring(1)).contains(data.substring(1))){
                return true;
            } else if((data.substring(1)).contains(whitelist[i].substring(1))){
                return true;
            }
        }
        return false;
    }
}
