package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
// Здесь устанавливаем данные пользователя: имя, картинка, телефон, марка машины
public class SettingsActivity extends AppCompatActivity {

    String getType, checker=""; // getType- для получения интента с стрингом либо Drivers, или Customers
                                // checker - для проверки был ли клик по кнопке сменить картинку
    private ImageView close_button, save_button; // кнопки закрытия активити и сохранения данных
    private CircleImageView profile_image;// круглое фигура для фото
    private TextView change_photo_button; // кнопка сменить фото
    private EditText name, phone, car_name;
    private Uri imageUri; // переменная для взятия местонахождения картинки на локальном устройстве
    private String myUrl = ""; // URL картинки в Firebase Storage для сохранение этого значения на FirebaseDatabase
    private StorageTask uploadTask; // для проверки выполнелось ли сохранение картинки в Firebase Storage
    private StorageReference storageProfileImageRef; // ссылка на папку и название Для сохранения картинки в Firebase Storage
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");//создаем папку в Firebase Storage

        close_button = (ImageView) findViewById(R.id.close_button);
        save_button = (ImageView) findViewById(R.id.save_button);
        profile_image = (CircleImageView)findViewById(R.id.profile_image);
        change_photo_button = (TextView)findViewById(R.id.change_photo_btn);
        name = (EditText)findViewById(R.id.name);
        phone = (EditText)findViewById(R.id.phone);
        car_name = (EditText)findViewById(R.id.car_name);

        if(getType.equals("Drivers")){
            car_name.setVisibility(View.VISIBLE);
        }

        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getType.equals("Drivers")){
                    startActivity(new Intent(SettingsActivity.this, DriversMapActivity.class));
                }else{
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                }
            }
        });

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checker.equals("clicked")){
                    ValidateControllers(); //функция для проверки заполнение всех полей с учетом выбора картинки и вызыва функции сохранение данных
                }else {
                    ValidateAndSaveOnlyInformation();//функция для проверки заполнение всех полей без учетом выбора картинки
                }
            }
        });

        change_photo_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "clicked";
                CropImage.activity().setAspectRatio(1,1).start(SettingsActivity.this);// переходим в хранилище ПК
            }
        });

        //Функция для взятия данных пользователя из FireBaseDatabase при их наличии
        getUserInformation();
    }




    // функция срабатывает при выборе картинки из хранилища ПК
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data!=null){
            CropImage.ActivityResult result = CropImage.getActivityResult(data); //берем картинку из ПК
            imageUri = result.getUri();// берем Uri(местонахождение)
            profile_image.setImageURI(imageUri); // устанавливаем картинку в круг
        }else {
            if(getType.equals("Drivers")){
                startActivity(new Intent(SettingsActivity.this, DriversMapActivity.class));
            }else{
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }

            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    //функция для проверки заполнение всех полей с учетом выбора картинки и вызыва функции сохранение данных
    private void ValidateControllers(){
        if(TextUtils.isEmpty(name.getText().toString()))
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        else if(TextUtils.isEmpty(phone.getText().toString()))
            Toast.makeText(this, "Заполните поле номер", Toast.LENGTH_SHORT).show();
        else if(getType.equals("Drivers") && TextUtils.isEmpty(car_name.getText().toString()))
            Toast.makeText(this, "Заполните поле модель машины", Toast.LENGTH_SHORT).show();
        else if(checker.equals("clicked")){
            //функция для сохранения картинки на FirebaseStorage и отправка его url и данные на FirebaseDatabase
            uploadProfileImage();
        }
    }

    //функция для сохранения картинки на FirebaseStorage и отправка его url и данные на FirebaseDatabase
    private void uploadProfileImage() {
        if(imageUri!=null){//Uri был взят при выборе картинки
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Загрузка");
            progressDialog.setMessage("Подождите");
            progressDialog.show();

            final  StorageReference fileRef = storageProfileImageRef.child(mAuth.getCurrentUser().getUid() + "jpg");// создаем в папке Profile Pictures имя для картинки пользователя
            uploadTask = fileRef.putFile(imageUri); // кладем в него картинку из хранилище ПК по адресу
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {// проверка сохранения
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task <Uri> task) {//метод при успешной сохранении
                    if(task.isSuccessful()){
                        Uri downloadUri = task.getResult();//берем адрес  картинки c FirebaseStorage
                        myUrl = downloadUri.toString();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid",mAuth.getCurrentUser().getUid());
                        userMap.put("name",name.getText().toString());
                        userMap.put("phone",phone.getText().toString());
                        userMap.put("image",myUrl);

                        if(getType.equals("Drivers")){
                            userMap.put("car",car_name.getText().toString());
                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
                        progressDialog.dismiss();
                        if(getType.equals("Drivers")){
                            startActivity(new Intent(SettingsActivity.this, DriversMapActivity.class));
                        }else{
                            startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                        }
                    }
                }
            });
        }else{
            Toast.makeText(this, "Изображение не выбрано!", Toast.LENGTH_SHORT).show();
        }
    }

    //функция для проверки заполнение всех полей без учетом выбора картинки
    private void ValidateAndSaveOnlyInformation() {
        if(TextUtils.isEmpty(name.getText().toString()))
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        else if(TextUtils.isEmpty(phone.getText().toString()))
            Toast.makeText(this, "Заполните поле номер", Toast.LENGTH_SHORT).show();
        else if(getType.equals("Drivers") && TextUtils.isEmpty(car_name.getText().toString()))
            Toast.makeText(this, "Заполните поле модель машины", Toast.LENGTH_SHORT).show();
        else{
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid",mAuth.getCurrentUser().getUid());
            userMap.put("name",name.getText().toString());
            userMap.put("phone",phone.getText().toString());

            if(getType.equals("Drivers")){
                userMap.put("car",car_name.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
            if(getType.equals("Drivers")){
                startActivity(new Intent(SettingsActivity.this, DriversMapActivity.class));
            }else{
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
        }
    }
    //Функция для взятия данных пользователя из FireBaseDatabase при их наличии
    private void getUserInformation() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    String nameSt = snapshot.child("name").getValue().toString();
                    String phoneSt = snapshot.child("phone").getValue().toString();

                    if(getType.equals("Drivers")){
                        String carnameSt = snapshot.child("carname").getValue().toString();
                        car_name.setText(carnameSt);
                    }
                    if(snapshot.hasChild("image")){
                        String imageSt = snapshot.child("image").getValue().toString();
                        Picasso.get().load(imageSt).into(profile_image);// Метод для загрузки картинки по адресу в круг https://github.com/square/picasso
                    }

                    name.setText(nameSt);
                    phone.setText(phoneSt);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}