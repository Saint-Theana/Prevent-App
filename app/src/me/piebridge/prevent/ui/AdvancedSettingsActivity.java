package me.piebridge.prevent.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import me.piebridge.prevent.R;
import me.piebridge.prevent.common.PreventIntent;
import me.piebridge.prevent.ui.util.DeprecatedUtils;
import me.piebridge.prevent.ui.util.PreventUtils;

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
