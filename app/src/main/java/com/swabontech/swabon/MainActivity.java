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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient locClient;
    private LocationCallback locationCallback;
    private Location l;
    private LocationManager lm;
    private GeofencingClient geo;
    private List<Geofence> gs = new ArrayList<>();
    private PlacesClient placesClient;

    private PendingIntent geofencePendingIntent;

    private final int LOCATION_CODE = 100;
    private final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private boolean lSet = false;
    boolean yy = true;

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

        locClient = LocationServices.getFusedLocationProviderClient(this);
        geo = LocationServices.getGeofencingClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                l = locationResult.getLocations().get(0);
            }
        };

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        // Create a new Places client instance
        placesClient = Places.createClient(this);

        createNotificationChannel();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        getCurrentLocation();
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
        //Get zones within a five mile radius

        //currently example zones
        char types[] = {0,1,2};
        CircleOptions c1 = new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),100,100))
                .radius(100);
        CircleOptions c2 = new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),100,-100))
                .radius(100);
        CircleOptions c3 = new CircleOptions().center(translate(l.getLatitude(),l.getLongitude(),-100,0))
                .radius(100);
        CircleOptions c[] = {c1,c2,c3};
        gs.clear();

        for (int i = 0; i < c.length; i++) {
            createZone(types[i], c[i]);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        geo.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        // ...
                        Toast.makeText(MainActivity.this, "Geos made", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        // ...
                    }
                });
    }

    private void createZone(char type, CircleOptions o) {
        int col;
        switch (type) {
            case 0:
                col = Color.argb(100, 0, 255, 0);
                break;
            case 1:
                col = Color.argb(100, 0, 255, 255);
                break;
            case 2:
                col = Color.argb(100, 255, 0, 0);
                break;
            default:
                col = Color.BLACK;
        }
        mMap.addCircle(o.fillColor(col).strokeWidth(0));
        gs.add(new Geofence.Builder()
                .setRequestId(Integer.toString(type)+Integer.toString(gs.size()))
                .setCircularRegion(o.getCenter().latitude,
                                    o.getCenter().longitude,
                            (float) o.getRadius())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                            | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(10000)
                .build());
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder b = new GeofencingRequest.Builder();
        b.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|GeofencingRequest.INITIAL_TRIGGER_DWELL);

        b.addGeofences(gs);
        return b.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
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
                if (yy) {
                    setMock(50.3709, -4.1317, true);
                } else {
                    setMock(50.3714883, -4.132739, true);
                }
                yy = !yy;
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void getCurrentLocation() {
        lm = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = String.valueOf(lm.getBestProvider(criteria, true)).toString();


        setMock(50.3714883, -4.132739, false);

        //You can still do this if you like, you might get lucky:
        Location loc = lm.getLastKnownLocation(bestProvider);
        if (loc != null) {
            setLocation(loc);
        }
        else{
            lm.requestLocationUpdates(bestProvider, 1000, 0, this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Toast.makeText(this, "Place: " + place.getName() + ", " + place.getId(), Toast.LENGTH_LONG).show();
                l.setLatitude(place.getLatLng().latitude);
                l.setLongitude(place.getLatLng().longitude);
                moveMap(true);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Swabon", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //Hey, a non null location! Sweet!

        //remove location callback:
        if (lm != null) {
            //lm.removeUpdates(this);
            setLocation(location);
        }
    }

    private void setLocation(Location loc) {
        lSet = true;
        l = loc;
        Log.e("swabon", "GPS is on");
        Log.e("swabon", l.getLatitude() + " " + l.getLongitude());

        /*LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());*/


        moveMap(true);
    }

    private void setMock(double latitude, double longitude, boolean x) {
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
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
