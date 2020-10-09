package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegLoginActivity extends AppCompatActivity {

    //Все тоже самое как в DriverRegLoginActivity, но при РЕгистрации создаем в Users Таблицу Customers и туда располагаем данные клиента по Id

    TextView CustomerTitleTV, DontHaveAccountTV;
    EditText CustomerEmailET,CustomerPasswordET;
    Button CustomerSignInBT,CustomerSignUpBT;

    FirebaseAuth mAuth;
    DatabaseReference CustomerDatabaseRef;
    String onlineCustomerId;
    ProgressDialog loadingBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser!=null){
            Toast.makeText(CustomerRegLoginActivity.this, "User is login", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(CustomerRegLoginActivity.this, "User is logout!", Toast.LENGTH_SHORT).show();
        }

        CustomerTitleTV = (TextView)findViewById(R.id.CustomerTitleTV);
        DontHaveAccountTV =(TextView)findViewById(R.id.DontHaveAccountTV);

        CustomerEmailET = (EditText)findViewById(R.id.CustomerEmailET);
        CustomerPasswordET = (EditText)findViewById(R.id.CustomerPasswordET);

        CustomerSignInBT = (Button)findViewById(R.id.CustomerSignInBT);
        CustomerSignUpBT = (Button)findViewById(R.id.CustomerSignUpBT);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        CustomerSignUpBT.setVisibility(View.INVISIBLE);
        CustomerSignUpBT.setEnabled(false);

        DontHaveAccountTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomerSignUpBT.setVisibility(View.VISIBLE);
                CustomerSignUpBT.setEnabled(true);

                CustomerSignInBT.setVisibility(View.INVISIBLE);
                CustomerSignInBT.setEnabled(false);

                DontHaveAccountTV.setVisibility(View.INVISIBLE);
                DontHaveAccountTV.setEnabled(false);

                CustomerTitleTV.setText("Регистрация водителя");
            }
        });

        CustomerSignInBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = CustomerEmailET.getText().toString();
                String password = CustomerPasswordET.getText().toString();

                SignInCustomer(email, password);
            }
        });

        CustomerSignUpBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = CustomerEmailET.getText().toString();
                String password = CustomerPasswordET.getText().toString();

                RegisterCustomer(email, password);
            }
        });
    }

    private void SignInCustomer(String email, String password) {
        loadingBar.setTitle("Вход клиента!");
        loadingBar.setMessage("Подождите...");
        loadingBar.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(CustomerRegLoginActivity.this, "Вы успешно ввошли в свой аккаунт!", Toast.LENGTH_SHORT).show();
                    Intent intentCustomerMap = new Intent(CustomerRegLoginActivity.this, CustomersMapActivity.class);
                    startActivity(intentCustomerMap);
                    finish();
                }
                else {
                    Toast.makeText(CustomerRegLoginActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
                }
                loadingBar.dismiss();
            }
        });
    }

    private void RegisterCustomer(String email, String password) {

        loadingBar.setTitle("Регистрация клиента!");
        loadingBar.setMessage("Подождите...");
        loadingBar.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    onlineCustomerId = mAuth.getCurrentUser().getUid();
                    CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(onlineCustomerId);
                    CustomerDatabaseRef.setValue(true);

                    Intent intentCustomerMap = new Intent(CustomerRegLoginActivity.this, CustomersMapActivity.class);
                    startActivity(intentCustomerMap);
                    finish();

                    Toast.makeText(CustomerRegLoginActivity.this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show();

                }
                else {
                    Toast.makeText(CustomerRegLoginActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
                }
                loadingBar.dismiss();
            }
        });
    }
}