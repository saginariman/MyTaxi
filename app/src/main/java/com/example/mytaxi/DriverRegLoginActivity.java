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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegLoginActivity extends AppCompatActivity {

    TextView TitleTV, DontHaveAccountTV;
    EditText driverEmailET,driverPasswordET;
    Button driverSignInBT,driverSignUpBT;

    FirebaseAuth mAuth; // Для Аутифекации пользователей на FireBase
    DatabaseReference DriverDatabaseRef; // Ссылка базу данных для FireBase, Путь
    String onlineDriverId; //Id нашего данного воделя в FirebaseAuth
    ProgressDialog loadingBar; // Диалог Окно при загрузке Входа или Регистрации
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_login);

        TitleTV = (TextView)findViewById(R.id.TitleTV);
        DontHaveAccountTV =(TextView)findViewById(R.id.DontHaveAccountTV);

        driverEmailET = (EditText)findViewById(R.id.driverEmailET);
        driverPasswordET = (EditText)findViewById(R.id.driverPasswordET);

        driverSignInBT = (Button)findViewById(R.id.driverSignInBT);
        driverSignUpBT = (Button)findViewById(R.id.driverSignUpBT);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        driverSignUpBT.setVisibility(View.INVISIBLE); //При имении аккаунта (входе) прячем кнопку Регистрации
        driverSignUpBT.setEnabled(false); // Делаем не Кликабельной

        DontHaveAccountTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // Слушатель при нажатии на ссылку (НЕТ Аккаунта?)
                driverSignUpBT.setVisibility(View.VISIBLE);
                driverSignUpBT.setEnabled(true);

                driverSignInBT.setVisibility(View.INVISIBLE);
                driverSignInBT.setEnabled(false);

                DontHaveAccountTV.setVisibility(View.INVISIBLE);
                DontHaveAccountTV.setEnabled(false);

                TitleTV.setText("Регистрация водителя");
            }
        });

        driverSignInBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // Слушатель для Кнопки Входа
                String email = driverEmailET.getText().toString();
                String password = driverPasswordET.getText().toString();

                SignInDriver(email, password); // Функция Входа
            }
        });

        driverSignUpBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //Слушатель для кнопки Регистрация
                String email = driverEmailET.getText().toString();
                String password = driverPasswordET.getText().toString();

                RegisterDriver(email, password);//Функция Регистрации
            }
        });
    }

    private void SignInDriver(String email, String password) { // Функция Входа
        loadingBar.setTitle("Вход водителя!");
        loadingBar.setMessage("Подождите...");
        loadingBar.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(DriverRegLoginActivity.this, "Вы вошли свой аккаунт!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                    startActivity(intent);finish();
                }
                else {
                    Toast.makeText(DriverRegLoginActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
                }
                loadingBar.dismiss();
            }
        });
    }

    private void RegisterDriver(String email, String password) {//Функция Регистрации

        loadingBar.setTitle("Регистрация водителя!");
        loadingBar.setMessage("Подождите...");
        loadingBar.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    onlineDriverId = mAuth.getCurrentUser().getUid();
                    //Создаем в базе данных таблицу Users внутри таблицу Drivers и туда вносим Id данного водителя делаем ее true
                    DriverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverId);
                    DriverDatabaseRef.setValue(true);

                    Intent intent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                    startActivity(intent);finish();

                    Toast.makeText(DriverRegLoginActivity.this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show();

                }
                else {
                    Toast.makeText(DriverRegLoginActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
                }
                loadingBar.dismiss();
            }
        });
    }
}