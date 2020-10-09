package com.example.mytaxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    Button driverButton, customerButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        driverButton = (Button)findViewById(R.id.driverButton);
        customerButton = (Button)findViewById(R.id.customerButton);

        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentDriverButton = new Intent(WelcomeActivity.this, DriverRegLoginActivity.class);
                startActivity(intentDriverButton);
                finish();
            }
        });

        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentCustomerButton = new Intent(WelcomeActivity.this, CustomerRegLoginActivity.class);
                startActivity(intentCustomerButton);
                finish();
            }
        });
    }
}