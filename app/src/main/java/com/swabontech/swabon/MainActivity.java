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
import android.content.Context;
import android.content.Intent;
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
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*
    * Check if in zone
    * Notifications
    * In background location change
    * Get within 5 mile radius
    * Settings and Terms of Use (and remove first page from page stack)
    * Pretty design
    * Pretty code
    * */

    private GoogleMap mMap;
    private FusedLocationProviderClient locClient;
    private LocationCallback locationCallback;
    private Location l;
    private PlacesClient placesClient;

    private final int LOCATION_CODE = 100;
    private final int AUTOCOMPLETE_REQUEST_CODE = 1;
    boolean yy = true;
    int notificationId = 0;
    int currentZone = -1;

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

        createNotificationChannel();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void checkPermission() {

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e("swabon", "Permission denied");
            makeRequest();
        } else {
            getCurrentLocation();
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
                getCurrentLocation();
            }
        }
    }

    public void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Zone Entry Notifications";
            String description = "Notify when you enter a new zone and give tips";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("swabon_notifications_01", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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

    private void moveMap(boolean x) {
        LatLng current = new LatLng(l.getLatitude(), l.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f));
        if  (x) {
            mMap.clear();
        }
        mMap.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(), l.getLongitude())).title("Current"));
        if  (x) {
            setupZones();
        }
    }

    private LatLng translate(double lat, double lon, double metersX, double metersY) {
        metersX /= 1000;
        metersY /= 1000;
        LatLng x = new LatLng(lat+(metersY/111.111), lon+(metersX/(111.111 * Math.cos(lat))));
        Log.e("swabon", lat + " " + lon + " " + x.latitude + " " + x.longitude);
        return x;
    }



    private void setupZones() {
        //Get zones within a five mile radius, should be closer zones first

        //currently example zones
        List<Integer> types = new ArrayList<>();
        types.add(0);
        types.add(1);
        if (!yy)
            types.add(2);

        List<CircleOptions> c = new ArrayList<>();
        c.add(new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),100,100))
                .radius(100));
        c.add(new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),200,-200))
                .radius(100));
        if (!yy)
            c.add(new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),-100,0)).radius(100));

        for (int i = 0; i < c.size(); i++) {
            createZone(types.get(i), c.get(i));
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Check if marker is zone
        //First, get indexes of all 2s, 1s, and 0s. Test 2s first and see if in zone. If it is, raise notif, if not move on
        List<Integer> twos = new ArrayList<>();
        List<Integer> ones = new ArrayList<>();
        List<Integer> zeros = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            switch (types.get(i)) {
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
        for (int two : twos) { if (inside(c.get(two))) { type = 2; x = false; break; } }
        for (int one : ones) { if (x && inside(c.get(one))) { type = 1; x =  false; break; } }
        for (int zero : zeros) { if (x && inside(c.get(zero))) { type = 0; break; } }

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

            sendNotification(text);
        }
    }

    private void createZone(int type, CircleOptions o) {
        int col;
        switch (type) {
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
        mMap.addCircle(o.fillColor(col).strokeWidth(0));
    }

    private boolean inside(CircleOptions centerPoint) {
        //radius is in meters and this function is in km
        double rad = centerPoint.getRadius()/1000;

        double ky = 40000 / 360;
        double kx = Math.cos(Math.PI * centerPoint.getCenter().latitude / 180.0) * ky;
        double dx = Math.abs(centerPoint.getCenter().longitude - l.getLongitude()) * kx;
        double dy = Math.abs(centerPoint.getCenter().latitude - l.getLatitude()) * ky;
        return Math.sqrt(dx * dx + dy * dy) <= rad;
    }

    public void sendNotification(String text) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "swabon_notifications_01")
                .setSmallIcon(R.drawable.swabon)
                .setContentTitle("New Zone")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId++, builder.build());
    }

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
                yy = false;


                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void getCurrentLocation() {
        locClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("Swabon", "No location from update");
                    Toast.makeText(MainActivity.this, "No location from update", Toast.LENGTH_SHORT).show();
                    return;
                }
                setLocation(locationResult.getLocations().get(0));
            }
        };

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        // Create a new Places client instance
        placesClient = Places.createClient(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(300000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        locClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        Log.e("Swabon", "No location");
                        if (location != null) {
                            // Logic to handle location object
                            setLocation(location);
                        }
                    }
                });



        /*lm = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = String.valueOf(lm.getBestProvider(criteria, true)).toString();


        //setMock(50.3714883, -4.132739, false);

        //You can still do this if you like, you might get lucky:
        Location loc = lm.getLastKnownLocation(bestProvider);
        if (loc != null) {
            setLocation(loc);
        }
        else{
            lm.requestLocationUpdates(bestProvider, 1000, 0, this);
        }*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Toast.makeText(this, "Place: " + place.getName() + ", " + place.getId(), Toast.LENGTH_LONG).show();
                setLocation(place.getLatLng().latitude, place.getLatLng().longitude);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Swabon", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private void setLocation(Location loc) {
        l = loc;
        Log.e("swabon", "GPS is on");
        Log.e("swabon", l.getLatitude() + " " + l.getLongitude());

        moveMap(true);
    }

    private void setLocation(double lat, double lon) {
        l.setLatitude(lat);
        l.setLongitude(lon);
        Log.e("swabon", "GPS is on");
        Log.e("swabon", lat + " " + lon);

        moveMap(true);
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
