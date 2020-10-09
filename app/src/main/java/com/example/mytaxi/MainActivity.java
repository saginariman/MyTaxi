package com.example.mytaxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();

                try {
                    sleep(4000);
                }catch (Exception e){

                }
                finally {
                    Intent intentToWelcomeActivity = new Intent(MainActivity.this, WelcomeActivity.class);
                    startActivity(intentToWelcomeActivity);
                }
            }
        };

        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}