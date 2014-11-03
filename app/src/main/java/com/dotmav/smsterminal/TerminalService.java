package com.dotmav.smsterminal;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalService extends Service{

    private static final int TERMINAL_REQUEST = 1000;

    private boolean secured = false;

    private String command = null;
    private String number = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        secured = intent.getBooleanExtra("secured", false);
        command = intent.getStringExtra("command");
        number = intent.getStringExtra("number");

        /*final String[] all_commands = getResources().getStringArray(R.array.commands);
        for(int i = 0; i < all_commands.length; i++){
            if(all_commands[i].equals(command) && secured) execute();
        }*/

        execute();

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
        } else if(cmd[1].equals("bash")){
            String options = command.replace(cmd[0]+' '+cmd[1]+' ', "");
            bash(options);
        } else {
            Log.i("DEV", "CMD not exists");
        }
    }

    private void sound(String arg){
        AudioManager am = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
        if(arg.equals("on")){
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
        } else if(arg.equals("off")){
            am.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void data(String arg){
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
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(arg.equals("on") && !wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        } else if(arg.equals("off") && wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
        }
    }

    private void sync(String arg){
        if(arg.equals("on")){
            ContentResolver.setMasterSyncAutomatically(true);
        } else if(arg.equals("off")){
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }

    private void bash(String arg){
        File folder = new File(Environment.getExternalStorageDirectory() + "/smst");
        boolean exists = true;
        if(!folder.exists()){
            exists = folder.mkdir();
        }
        if(exists) {
            String num = "0000";
            for (File f : folder.listFiles()) {
                if(f.isFile()) {
                    String name = f.getName().substring(5);
                    if(num.compareTo(name) < 0){
                        num = name;
                    }
                }
            }
            int new_file_index = Integer.parseInt(num)+1;
            final String number = String.format("%04d", new_file_index);
            arg = "cd /sdcard; " + arg + " > /sdcard/smst/smstl"+number+"; echo '...EOF...' >> /sdcard/smst/smstl"+number+"; exit;";
            startATE(arg);
            try {
                File file = new File("/sdcard/smst/smstl"+number);
                while (!file.exists()){}
                final StringBuilder output = new StringBuilder();
                try {
                    boolean skip = false;
                    final RandomAccessFile r = new RandomAccessFile(file, "r");
                    String line = null;
                    while ((line = r.readLine()) != null) {
                        output.append(line);
                        output.append('\n');
                        Log.i("smstl"+number, line);
                        if (line.equals("...EOF...")) {
                            skip = true;
                            //startATE("rm /sdcard/smst/smstl"+number+";");
                            //Log.i("OUTPUT", output.toString());
                            //SmsManager smsManager = SmsManager.getDefault();
                            //smsManager.sendTextMessage(number, null,output.toString(), null, null);
                            break;
                        }
                    }
                    if(!skip) {
                        r.seek(r.getFilePointer());
                        final Timer timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                Log.i("DEV", "new check");
                                String line = null;
                                try {
                                    while ((line = r.readLine()) != null) {
                                        output.append(line);
                                        output.append('\n');
                                        Log.i("smstl" + number, line);
                                        if (line.equals("...EOF...")) {
                                            timer.cancel();
                                            //startATE("rm /sdcard/smst/smstl"+number+";");
                                            //Log.i("OUTPUT", output.toString());
                                            //SmsManager smsManager = SmsManager.getDefault();
                                            //smsManager.sendTextMessage(number, null,output.toString(), null, null);
                                            break;
                                        }
                                    }
                                    r.seek(r.getFilePointer());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 0, 1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startATE(String cmd){
        Log.i("DEV", "startATE( "+cmd+" )");
        Intent bash = new Intent("jackpal.androidterm.RUN_SCRIPT");
        bash.addCategory(Intent.CATEGORY_DEFAULT);
        bash.putExtra("jackpal.androidterm.iInitialCommand", cmd);
        bash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(bash);
    }
}
