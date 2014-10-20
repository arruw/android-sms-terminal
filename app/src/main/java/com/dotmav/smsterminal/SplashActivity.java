package com.dotmav.smsterminal;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final ImageView splash_text = (ImageView) findViewById(R.id.splash_text);
        final TextView splash_dots = (TextView) findViewById(R.id.splash_dots);

        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.splash_animator);
        set.setTarget(splash_text);
        set.start();

        Thread dots = new Thread() {
            @Override
            public void run() {
                try {
                    int count = 0;
                    while(count < 15) {
                        sleep(250);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                splash_dots.setText(splash_dots.getText().toString() + "..");
                            }
                        });
                        count++;
                    }
                    Intent end_splash = new Intent(SplashActivity.this, PreferencesActivity.class);
                    SplashActivity.this.startActivity(end_splash);
                    SplashActivity.this.finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        dots.start();
    }

}
