package com.swabontech.swabon;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


    }

    public void getStarted(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Device Registration");
        alert.setMessage("Please enter your device code");

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!input.getText().toString().isEmpty()) {
                    SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("device_code", input.getText().toString());
                    editor.commit();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled.
            }
        });

        alert.show();
    }
}
