package me.piebridge.prevent.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import me.piebridge.prevent.R;

/**
 * Created by thom on 15/10/3.
 */
public class AdvancedSettingsActivity extends AppCompatActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar=findViewById(R.id.activitysettingsToolbar1);
        setSupportActionBar(toolbar);
    }


}
