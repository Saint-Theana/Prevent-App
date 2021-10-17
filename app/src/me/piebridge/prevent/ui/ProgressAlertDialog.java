package me.piebridge.prevent.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import me.piebridge.prevent.R;

public class ProgressAlertDialog extends AlertDialog {

    public ProgressAlertDialog(@NonNull Context context) {
        super(context);
        setView(LayoutInflater.from(context).inflate(R.layout.progress_dialog_horizonal, null));

    }


    public void setMax(int max) {
        ProgressBar mPb = findViewById(R.id.dialog_progressbar);
        mPb.setMax(max);
    }

    public void setProgress(int progress) {
        ProgressBar mPb = findViewById(R.id.dialog_progressbar);
        mPb.setProgress(progress);
    }
}
