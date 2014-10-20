package com.dotmav.smsterminal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;


public class ForResult extends Activity {

    private static final int CAMERA_REQUEST = 1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int request = getIntent().getIntExtra("request", 0);

        switch(request){
            case 0: break;
            case CAMERA_REQUEST:
                try {
                    String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smst";
                    File dir = new File(file_path);
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, "pic_" + DateFormat.getDateTimeInstance().format(new Date()) + ".png");
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);

                    Uri outputFileUri = Uri.fromFile(file);

                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                } catch(Exception e){
                    e.printStackTrace();
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK){
            Log.i("DEV", "pic taken and saved");
        }
    }
}
