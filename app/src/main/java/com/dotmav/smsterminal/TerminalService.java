package com.dotmav.smsterminal;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TerminalService extends Service{

    private boolean secured = false;

    private String command = null;
    private String number = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        secured = intent.getBooleanExtra("secured", false);
        command = intent.getStringExtra("command");
        number = intent.getStringExtra("number");

        final String[] all_commands = getResources().getStringArray(R.array.commands);
        for(int i = 0; i < all_commands.length; i++){
            if(all_commands[i].equals(command) && secured) execute();
        }

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void execute(){
        String[] cmd = command.split(" ");
        if(cmd[1].equals("sound")){
            sound(cmd[2]);
        } else if(cmd[1].equals("data")){
            data(cmd[2]);
        } else if(cmd[1].equals("wifi")){
            wifi(cmd[2]);
        } else if(cmd[1].equals("sync")){
            sync(cmd[2]);
        } else if(cmd[1].equals("help")){
            help();
        }
    }

    private void sound(String arg){
        Log.i("DEV", "sound("+arg+")");
        AudioManager am = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
        if(arg.equals("on")){
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
        } else if(arg.equals("off")){
            am.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void data(String arg){
        Log.i("DEV", "data("+arg+")");
        try {
            final ConnectivityManager conman = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
            connectivityManagerField.setAccessible(true);
            final Object connectivityManager = connectivityManagerField.get(conman);
            final Class connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            if(arg.equals("on")){
                setMobileDataEnabledMethod.invoke(connectivityManager, true);
            } else if(arg.equals("off")){
                setMobileDataEnabledMethod.invoke(connectivityManager, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void wifi(String arg){
        Log.i("DEV", "wifi("+arg+")");
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(arg.equals("on") && !wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        } else if(arg.equals("off") && wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
        }
    }

    private void sync(String arg){
        Log.i("DEV", "sync("+arg+")");
        if(arg.equals("on")){
            ContentResolver.setMasterSyncAutomatically(true);
        } else if(arg.equals("off")){
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }

    private void help(){
        Log.i("DEV", "help()");
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, getResources().getString(R.string.smst_help), null, null);
    }

}
