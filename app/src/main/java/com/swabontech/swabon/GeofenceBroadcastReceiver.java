package com.swabontech.swabon;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    Context c = null;
    int notificationId = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        c = context;
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        Log.e("swabon", "hey");
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            Toast.makeText(context, Integer.toString(triggeringGeofences.size()), Toast.LENGTH_SHORT).show();
            short type = 0;
            for (Geofence g : triggeringGeofences) {
                String s = g.getRequestId();
                if (s.charAt(0)-0x30 > type) {
                    type = (short) (s.charAt(0)-0x30);
                }
                Log.e("swabon", s + " " + type);
            }

            String text = "";
            switch (type) {
                case 0:
                    text = "You're in a safe zone but still be careful";
                    break;
                case 1:
                    text = "You're in an intermediate zone so make sure you have a mask and hand sanitizer";
                    break;
                case 2:
                    text = "You're in a hot zone so make sure you're covered up and don't spend to long there";
                    break;
            }

            sendNotification(text);
        } else {
            // Log the error.
            //Log.e(TAG, getString(R.string.geofence_transition_invalid_type,
            //geofenceTransition));
        }
    }

    public void sendNotification(String text) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(c, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, "swabon_notifications_01")
                .setSmallIcon(R.drawable.swabon)
                .setContentTitle("New Zone")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(c);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId++, builder.build());
    }

}
