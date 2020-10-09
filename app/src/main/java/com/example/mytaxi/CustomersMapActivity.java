package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

//В этой активите находим местоположение и дествия клиента
public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap; // Для подключения Гугл карт Обьект Гугл Мап
    GoogleApiClient googleApiClient; //Для отслежки по АПИ
    Location lastLocation;/*при изменении местоположение срабатывает функция onLocationChanged; у метода есть параметр location
                            который имеет значение местоположение, ив функции мы lastLocation= location, и lastLocation будем
                            использовать в customer_order_button.setOnClickListener;*/
    LocationRequest locationRequest; // используем для АПИ подключении, оно указывает параметры
    private int radius = 1;// Радиус поиска водителей
    private boolean driverFound =false;
    private String driverFoundId;
    Marker driverMarker;

    private Button customer_logout_button, customer_order_button,customer_settings_button;
    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseRef; // ссылка Для взятия клиента по ид
    private DatabaseReference DriversAvailableRef; // ссылка для взятия водителя по ид доступных
    private DatabaseReference DriversRef; // для взятия по ссылке водителя из общей папки Drivers
    private DatabaseReference DriversLocationRef; // ссылка на l водителя(позиция)

    String customerId;
    LatLng CustomerPosition;//обьект для обределения позиций клиента для маркера
    private boolean requestType = false;
    private ValueEventListener DriverLocationRefListener;
    GeoQuery geoQuery;
    Marker PickUpMarker;


    TextView driver_name, driver_phone, driver_car; //для панели мой такси данные
    CircleImageView profile_driver_image;
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mAuth = FirebaseAuth.getInstance();
        /*FirebaseUser currentUser = mAuth.getCurrentUser();*/
        customerId = mAuth.getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Request"); // создаем таблицу для клиентов на заказе
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available"); // для доступных водителей
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); // для водителя зявший этот заказ

        driver_name = (TextView) findViewById(R.id.driver_name);
        driver_phone=(TextView) findViewById(R.id.driver_phone);
        driver_car=(TextView)findViewById(R.id.driver_car);
        profile_driver_image = (CircleImageView)findViewById(R.id.profile_driver_image);
        relativeLayout = (RelativeLayout)findViewById(R.id.rell);
        relativeLayout.setVisibility(View.INVISIBLE);

        customer_logout_button = (Button)findViewById(R.id.customer_logout_button);
        customer_order_button = (Button)findViewById(R.id.customer_order_button);
        customer_settings_button = (Button)findViewById(R.id.customer_settings_button);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        customer_logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogoutCustomer();//выход из активити
                mAuth.signOut();// выход из FireBase
            }
        });
        //Функция для заказа кнопка клиент
        customer_order_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestType){// requestType==true
                    // при нажатии во второй раз на кнопку заказа отменяем заказ
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriversLocationRef.removeEventListener(DriverLocationRefListener);

                    if(driverFound){
                        DriversRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundId).child("CustomerRideId");
                        DriversRef.removeValue();
                        driverFoundId=null;
                    }
                    driverFound= false;
                    radius=1;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.removeLocation(customerId);

                    if(PickUpMarker!=null){
                        PickUpMarker.remove();
                    }

                    if(driverMarker!=null){
                        driverMarker.remove();
                    }

                    customer_order_button.setText("Вызвать такси");
                }else{
                    requestType = true;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef); // Отправляет в реальном времени данные клиента по этой ссылки в базе данных FireBase
                    geoFire.setLocation(customerId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude())); //установка

                    CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());// взятие позиций клиента

                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я здесь").icon(BitmapDescriptorFactory
                            .fromResource(R.drawable.user)));// маркер на карте

                    customer_order_button.setText("Поиск водителя...");
                    //функция для поиска водителя в радиусе
                    getNearbyDrivers();
                }

            }
        });

        customer_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;//подключение гугл карты
        //функция для создания АПИ, АПИ нужен для определения местоположения
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);
    }
    //определения местоположения пользователя по АПИ с требованиеми при подключении интервалым 1с
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //определения местоположения пользователя по АПИ с требованиеми здесь
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // при изменении местонахождении двигаем камеру
    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }
    //функция для создания АПИ, АПИ нужен для определения местоположения
    protected  synchronized  void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //выход из активити
    private void LogoutCustomer() {
        Intent intentWelcomeActivity = new Intent(CustomersMapActivity.this, CustomerRegLoginActivity.class);
        startActivity(intentWelcomeActivity);
        finish();
    }

    //функция для поиска водителя в радиусе
    private void getNearbyDrivers() {
        GeoFire geoFire =new GeoFire(DriversAvailableRef);// Создаем обьект реальной отслежки для свободных водителей
        //Обьект запроса поиска водителей данных с таблицы с местонахождением на определенном радиусе от клиента
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude),radius);
        geoQuery.removeAllListeners();// Чистка слушателя запроса
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {//слушатель на нахождения водителя
            @Override
            public void onKeyEntered(String key, GeoLocation location) {// при нахождении водителя
                if (!driverFound && requestType){// driverFound!=true driverFound == false
                    driverFound =true;
                    driverFoundId = key; // берем ИД водителя

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);//берем ссылку на водителя
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideId", customerId); // Хэш для использования для пивязки клиента в базе данных на найденного водителя
                    DriversRef.updateChildren(driverMap);//Добавляем в таблицу на строчку этого водителя нашего клиента связка
                    //Находим местонахождение водителя
                    GetDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound){
                    radius+=1;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    //Находим место нахождение водителя
    private void GetDriverLocation() {
        DriverLocationRefListener = DriversLocationRef.child(driverFoundId).child("l"). //из папки Driver Working берем location водителя и устанавливаем слушатель
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && requestType){
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double locationLat = 0;
                            double locationLng = 0;

                            customer_order_button.setText("Водитель найден");
                            gerDriverInformation();
                            relativeLayout.setVisibility(View.VISIBLE );

                            if(driverLocationMap.get(0) != null){
                                locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }

                            if(driverLocationMap.get(1) != null){
                                locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(locationLat, locationLng);
                            if(driverMarker != null){
                                driverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPosition.latitude);
                            location1.setLongitude(CustomerPosition.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);

                            float Distance = location1.distanceTo(location2);

                            if(Distance<100){
                                customer_order_button.setText("Ваше такси подьезжает "+ String.valueOf(Distance));
                            }else{
                                customer_order_button.setText("Растояние до такси "+ String.valueOf(Distance));
                            }



                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Ваше такси тут")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void gerDriverInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String nameSt = snapshot.child("name").getValue().toString();
                    String phoneSt = snapshot.child("phone").getValue().toString();
                    String carname = snapshot.child("carname").getValue().toString();
                    driver_name.setText(nameSt);
                    driver_phone.setText(phoneSt);
                    driver_car.setText(carname);

                    if(snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profile_driver_image);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}