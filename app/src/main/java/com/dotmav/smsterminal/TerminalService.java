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
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalService extends Service{

    private boolean secured = false;

    private String command = null;
    private String number = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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
            printData("CMD not exists\n");
        }
    }

    private void sound(String arg){
        printData("smsts sound "+arg);
        try {
            AudioManager am = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
            if (arg.equals("on")) {
                am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
            } else if (arg.equals("off")) {
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
            printData(" OK\n");
        } catch (Exception e) {
            printException(e);
            printData(" EX\n");
        }
    }

    private void data(String arg){
        printData("smsts data "+arg);
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
            printData(" OK\n");
        } catch (Exception e) {
            printException(e);
            printData(" EX\n");
        }

    }

    private void wifi(String arg){
        printData("smsts wifi "+arg);
        try {
            WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            if (arg.equals("on") && !wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            } else if (arg.equals("off") && wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
            printData(" OK\n");
        } catch (Exception e) {
            printException(e);
            printData(" EX\n");
        }
    }

    private void sync(String arg){
        printData("smsts sync "+arg);
        try {
            if (arg.equals("on")) {
                ContentResolver.setMasterSyncAutomatically(true);
            } else if (arg.equals("off")) {
                ContentResolver.setMasterSyncAutomatically(false);
            }
            printData(" OK\n");
        } catch (Exception e) {
            printException(e);
            printData(" EX\n");
        }
    }

    private void bash(String arg){
        printData("smsts bash "+arg);
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/smst");
            File folderBash = new File(Environment.getExternalStorageDirectory() + "/smst/bash");
            boolean exists = true;
            if (!folder.exists()) {
                exists = folder.mkdir();
                exists = folderBash.mkdir();
            } else if (!folderBash.exists()) {
                exists = folderBash.mkdir();
            }
            if (exists) {
                String num = "0000";
                for (File f : folderBash.listFiles()) {
                    if (f.isFile()) {
                        String name = f.getName().substring(5);
                        if (num.compareTo(name) < 0) {
                            num = name;
                        }
                    }
                }
                int new_file_index = Integer.parseInt(num) + 1;
                final String index = String.format("%04d", new_file_index);
                arg = "cd /sdcard; " + arg + " > /sdcard/smst/bash/smstl" + index + "; echo '...EOF...' >> /sdcard/smst/bash/smstl" + index + "; exit;";
                startATE(arg);

                File file = new File("/sdcard/smst/bash/smstl" + index);
                while (!file.exists()) {}
                final StringBuilder output = new StringBuilder();
                try {
                    boolean skip = false;
                    final RandomAccessFile r = new RandomAccessFile(file, "r");
                    String line = null;
                    while ((line = r.readLine()) != null) {
                        output.append(line);
                        output.append('\n');
                        Log.i("smstl" + index, line);
                        if (line.equals("...EOF...")) {
                            skip = true;
                            //startATE("rm /sdcard/smst/bash/smstl"+number+";");
                            //Log.i("OUTPUT", output.toString());
                            //SmsManager smsManager = SmsManager.getDefault();
                            //smsManager.sendTextMessage(number, null,output.toString(), null, null);
                            break;
                        }
                    }
                    if (!skip) {
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
                                        Log.i("smstl" + index, line);
                                        if (line.equals("...EOF...")) {
                                            timer.cancel();
                                            //startATE("rm /sdcard/smst/bash/smstl"+index+";");
                                            //Log.i("OUTPUT", output.toString());
                                            //SmsManager smsManager = SmsManager.getDefault();
                                            //smsManager.sendTextMessage(number, null,output.toString(), null, null);
                                            break;
                                        }
                                    }
                                    r.seek(r.getFilePointer());
                                } catch (Exception e) {
                                    printException(e);
                                }
                            }
                        }, 0, 1000);
                    }
                } catch (Exception e) {
                    printException(e);
                }
            }
            printData(" OK\n");
        } catch (Exception e) {
            printException(e);
            printData(" EX\n");
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

    private void printException(Exception e){
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/smst");
            boolean exists = true;
            if (!folder.exists()) {
                exists = folder.mkdir();
            }
            e.printStackTrace();

            if(exists) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                sw.toString();

                File exlog = new File("/sdcard/smst/smstExceptionLog");
                BufferedWriter output = new BufferedWriter(new FileWriter(exlog, true));
                output.append(sw.toString());
                output.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void printData(String data){
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/smst");
            boolean exists = true;
            if (!folder.exists()) {
                exists = folder.mkdir();
            }
            if(exists) {
                File dalog = new File("/sdcard/smst/smstDataLog");
                BufferedWriter output = new BufferedWriter(new FileWriter(dalog, true));
                output.append(data);
                output.close();
            }
        } catch (Exception e) {
            printException(e);
        }
    }
}
