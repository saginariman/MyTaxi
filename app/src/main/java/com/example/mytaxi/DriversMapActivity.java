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
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import java.util.List;
// В этой активити мы находим расположение водителя на карте
public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;// Для подключения Гугл карт Обьект Гугл Мап
    GoogleApiClient googleApiClient;//Для отслежки по АПИ
    Location lastLocation;
    LocationRequest locationRequest;// используем для АПИ подключении, оно указывает параметры

    private Button driver_settings_button, driver_logout_button;
    private FirebaseAuth mAuth;
    private DatabaseReference assignedCustomerRef, //ссылка для взятия хэша из Drivers он равен ид клиента
                                AssignedCustomerPositionRef;// //ссылка для взятия из CustomersRequest местоположение клиента по ид
    private String driverId, customerId="";
    Marker PickUpMarker;
    private boolean currentLogoutDriverStatus=false;
    private ValueEventListener AssignedCustomerPositionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);

        mAuth = FirebaseAuth.getInstance();
        /*FirebaseUser currentUser = mAuth.getCurrentUser();*/
        driverId = mAuth.getCurrentUser().getUid();
        driver_settings_button = (Button)findViewById(R.id.driver_settings_button);
        driver_logout_button = (Button)findViewById(R.id.driver_logout_button);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        driver_logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogoutDriver();//выход из активити
                DisconnectDriver();// осьановка передачи местоположения в реальном времени
                /*mAuth.signOut();*/
            }
        });

        driver_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriversMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });

        //метод для взятия ид хеша из Drivers
        getAssignedCustomerRequest();
    }
    //метод для взятия ид хеша из Drivers
    private void getAssignedCustomerRequest() {
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId)
                .child("CustomerRideId");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    customerId = snapshot.getValue().toString();

                    // метод взятия позиции клиента из CustomerRequest
                    getAssignedCustomerPosition();
                }
                else{
                    customerId="";
                    if(PickUpMarker!=null){
                        PickUpMarker.remove();
                    }

                    if(AssignedCustomerPositionListener != null){
                        AssignedCustomerPositionRef.removeEventListener(AssignedCustomerPositionListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    // метод взятия позиции клиента из CustomerRequest
    private void getAssignedCustomerPosition() {
        AssignedCustomerPositionRef = FirebaseDatabase.getInstance().getReference().child("Customers Request")
                .child(customerId).child("l");
        AssignedCustomerPositionListener = AssignedCustomerPositionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    List<Object>  customerPosition = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if(customerPosition.get(0) != null){
                        locationLat = Double.parseDouble(customerPosition.get(0).toString());
                    }

                    if(customerPosition.get(1) != null){
                        locationLng = Double.parseDouble(customerPosition.get(1).toString());
                    }

                    LatLng DriverLatLng = new LatLng(locationLat, locationLng);

                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Забрать клиента тут")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    // при изменении местонахождении двигаем камеру и отправляем местоположение в реалном времени в Drivers Available, если не имеем клиента,
    //или отправляем в Driver Working при имении клиента
    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
            lastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
            GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);


            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);


            switch(customerId){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailability.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailability.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
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
            DisconnectDriver();

    }

    // метод для остановки передачи местоположения в реальном времени в папку Drivers Available, удаление местонахождения
    private void DisconnectDriver() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvailability = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        try {
            GeoFire geoFire = new GeoFire(DriverAvailability);
            geoFire.removeLocation(userId);
        }catch(Exception e){
            Toast.makeText(this, "Error of GeoFire", Toast.LENGTH_LONG).show();
        }

    }
    //выход из активити
    private void LogoutDriver() {
        Intent intentWelcomeActivity = new Intent(DriversMapActivity.this, DriverRegLoginActivity.class);
        startActivity(intentWelcomeActivity);
        finish();
    }
}