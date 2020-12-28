package com.swabontech.swabon;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.EventBus;

public class LocationUpdateService extends Service {
    public static final String TAG = "swabon";

    private Intent notificationIntent;
    private PendingIntent pendingIntent;
    private String notif_id = "swabon_notifications_01";

    private Location l;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient locClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        locClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                l = locationResult.getLastLocation();
                Log.e("locreq swabon", "Lat: " + l.getLatitude() + " Lon: " + l.getLongitude());

                EventBus.getDefault().post(new LocationUpdateEvent(l));
            }
        };
        createLocationRequest();
    }

    public LocationUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        prepareForegroundNotification();

        startLocationUpdates();

        return START_STICKY;
    }

    public Location getCurrentLocation() {
        return l;
    }

    private void prepareForegroundNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Zone Entry Notifications";
            String description = "Notify when you enter a new zone and give tips";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel serviceChannel = new NotificationChannel(notif_id, name, importance);
            serviceChannel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }

        notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        startForeground(0, createNotification(""));
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, notif_id)
                .setSmallIcon(R.drawable.swabon)
                .setContentTitle("Swabon Zone")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent).build();
    }

    public void updateNotification(String text) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, createNotification(text));
    }


    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        locClient.removeLocationUpdates(locationCallback);
        //EventBus.getDefault().post(new LocationStoppedEvent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Service deatroyed");
        stopLocationUpdates();
    }

    private final IBinder binder = new LocationUpdateBinder();

    public class LocationUpdateBinder extends Binder {
        LocationUpdateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationUpdateService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public class LocationUpdateEvent {
        private Location location;

        public LocationUpdateEvent(Location locationUpdate) {
            this.location = locationUpdate;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }
}
