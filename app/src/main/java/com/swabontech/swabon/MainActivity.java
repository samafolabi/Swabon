package com.swabontech.swabon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*

    initmap (what happens when you move map)
    walking around (5 mile get, who hosts geofences, how to update geofences)
    request permissions nice-ities
    background location
    improve inside function algorithm
    perseistent notif?
    locationstoppedevent
    edit covid initial code
    what to do when app is closed, make sure service is running




    * Get within 5 mile radius
    search
    * Settings and Terms of Use (and remove first page from page stack)
    * Pretty design
    * Pretty code
    * */

    private GoogleMap mMap;
    private FusedLocationProviderClient locClient;
    private LocationCallback locationCallback;
    private Location l;
    boolean requestingLocationUpdates = false;
    LocationRequest locationRequest;
    String REQUESTING_LOCATION_UPDATES_KEY = "keykeybaby";

    private final int LOCATION_CODE = 100;
    private final int AUTOCOMPLETE_REQUEST_CODE = 1;
    int notificationId = 0;
    int currentZone = -1;
    public static final String TAG = "swabon";
    LocationUpdateService mService;
    boolean startUpdates = false;
    boolean initMap = false;
    boolean permissionChecked = false;

    class Zone {
        int type;
        double latitude;
        double longitude;
        int radius;

        public Zone (int t, double lat, double lon, int r) {
            type = t;
            latitude = lat;
            longitude = lon;
            radius = r;
        }
    }

    ArrayList<Zone> testZones = new ArrayList<Zone>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        SharedPreferences pref = getSharedPreferences("SWABON", MODE_PRIVATE);
        String code = pref.getString("device_code", "");
        if (code.isEmpty()) {
            Log.e("swabon", "empty");
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
        }

        testZones.add(new Zone(0, 40.401425000900005, -79.85945313317994100, 100));
        testZones.add(new Zone(1, 40.3996249991, -79.85945313317994,100));
        testZones.add(new Zone(2, 40.400525, -79.8584583,100));

        Intent intent = new Intent(this, LocationUpdateService.class);
        startService(intent);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //updateValuesFromBundle(savedInstanceState);
    }


    private void checkPermission() {
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        permissionChecked = true;

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e("swabon", "Permission denied");
            makeRequest();
        } else {
            if (!startUpdates) startUpdates = true;
        }
    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == LOCATION_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e("swabon", "Permission has been denied by user");
            } else {
                Log.e("swabon", "Permission has been granted by user");
                if (permissionChecked)
                    if (!startUpdates) startUpdates = true;
            }
        }
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkPermission();
    }

    private void moveMap() {
        //l = mService.getCurrentLocation();
        mMap.clear();
        LatLng current = new LatLng(l.getLatitude(), l.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f));
        mMap.addMarker(new MarkerOptions().position(current).title("Current"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        setupZones();
    }

    private void setupZones() {
        //Get zones within a five mile radius, should be closer zones first


        for (int i = 0; i < testZones.size(); i++) {
            createZone(testZones.get(i));
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Check if marker is zone
        //First, get indexes of all 2s, 1s, and 0s. Test 2s first and see if in zone. If it is, raise notif, if not move on
        List<Integer> twos = new ArrayList<>();
        List<Integer> ones = new ArrayList<>();
        List<Integer> zeros = new ArrayList<>();
        for (int i = 0; i < testZones.size(); i++) {
            switch (testZones.get(i).type) {
                case 2:
                    twos.add(i);
                    break;
                case 1:
                    ones.add(i);
                    break;
                case 0:
                    zeros.add(i);
                    break;
                default:
                    zeros.add(i);
            }
        }


        boolean x = true;
        int type = 0;
        for (int two : twos) { if (inside(testZones.get(two))) { type = 2; x = false; break; } }
        for (int one : ones) { if (x && inside(testZones.get(one))) { type = 1; x =  false; break; } }
        for (int zero : zeros) { if (x && inside(testZones.get(zero))) { type = 0; break; } }

        Log.e("Swabon", ""+type);

        if (type != currentZone) {
            currentZone = type;
            String text = "";
            switch (type) {
                case 2:
                    text = "You're in a hot zone so make sure you're covered up and don't spend to long there";
                    break;
                case 1:
                    text = "You're in an intermediate zone so make sure you have a mask and hand sanitizer";
                    break;
                case 0:
                default:
                    text = "You're in a safe zone but still be careful";
                    break;
            }

            mService.updateNotification(text);
        }


    }

    private void createZone(Zone o) {
        int col;
        switch (o.type) {
            case 0:
                col = Color.argb(100, 0, 255, 0);
                break;
            case 1:
                col = Color.argb(100, 0, 0, 255);
                break;
            case 2:
                col = Color.argb(100, 255, 0, 0);
                break;
            default:
                col = Color.BLACK;
        }
        mMap.addCircle(new CircleOptions().center(new LatLng(o.latitude, o.longitude))
                .radius(o.radius).fillColor(col).strokeWidth(0));
    }

    private boolean inside(Zone o) {
/*

        //radius is in meters and this function is in km
        double rad = o.radius/1000;

        double ky = 40000 / 360;
        double kx = Math.cos(Math.PI * o.latitude / 180.0) * ky;
        double dx = Math.abs(o.longitude - l.getLongitude()) * kx;
        double dy = Math.abs(o.latitude - l.getLatitude()) * ky;
        return Math.sqrt(dx * dx + dy * dy) <= rad;*/

        Location loc = new Location("");
        loc.setLatitude(o.latitude);
        loc.setLongitude(o.longitude);
        return loc.distanceTo(l) <= o.radius;
    }




    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationUpdateService.LocationUpdateEvent event) {
        if (startUpdates) {
            Log.e(TAG, "message received");
            l = event.getLocation();
            moveMap();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocationUpdateService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        EventBus.getDefault().unregister(this);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.e(TAG, "Service connected");
            LocationUpdateService.LocationUpdateBinder binder = (LocationUpdateService.LocationUpdateBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(TAG, "Service disconnected");
        }
    };





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

                Intent intent = new Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY, fields)
                        .build(this);
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
                return true;

            case R.id.settings:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...


                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Toast.makeText(this, "Place: " + place.getName() + ", " + place.getId(), Toast.LENGTH_LONG).show();
                //setLocation(place.getLatLng().latitude, place.getLatLng().longitude);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Swabon", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }





    /*private void setMock(double latitude, double longitude, boolean x) {
        lm.addTestProvider(LocationManager.GPS_PROVIDER,
                "requiresNetwork" == "",
                "requiresSatellite" == "",
                "requiresCell" == "",
                "hasMonetaryCost" == "",
                "supportsAltitude" == "",
                "supportsSpeed" == "",
                "supportsBearing" == "",
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE);

        Location newLocation = new Location(LocationManager.GPS_PROVIDER);

        if (x) {
            l.setLatitude(latitude);
            l.setLongitude(longitude);
        }

        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAltitude(0);
        newLocation.setAccuracy(500);
        newLocation.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        lm.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null,System.currentTimeMillis());

        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);

        if (x)
        moveMap(false);
    }*/
}

