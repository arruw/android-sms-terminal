package com.dotmav.smsterminal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;


public class ForResult extends Activity {

    private static final int TERMINAL_REQUEST = 1000;

    private String mHandle;
    private static final int REQUEST_WINDOW_HANDLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int request = getIntent().getIntExtra("request", 0);
        String command = getIntent().getStringExtra("command");

        switch(request){
            case 0: break;
            case TERMINAL_REQUEST:
                try {
                    Intent intent = new Intent("jackpal.androidterm.RUN_SCRIPT");
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.putExtra("jackpal.androidterm.iInitialCommand", command);
                    if (mHandle != null) {
                        // Identify the targeted window by its handle
                        intent.putExtra("jackpal.androidterm.window_handle",
                                mHandle);
                    }
                /* The handle for the targeted window -- whether newly opened
                   or reused -- is returned to us via onActivityResult()
                   You can compare it against an existing saved handle to
                   determine whether or not a new window was opened */
                    startActivityForResult(intent, REQUEST_WINDOW_HANDLE);
                } catch(Exception e){
                    e.printStackTrace();
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == TERMINAL_REQUEST && resultCode == RESULT_OK){
            Log.i("DEV", "read file");
        }

        if (requestCode == REQUEST_WINDOW_HANDLE && data != null) {
            mHandle = data.getStringExtra("jackpal.androidterm.window_handle");
            Log.i("DEV", "read file");
        }
    }
}
