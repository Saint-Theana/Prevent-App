package me.piebridge.prevent.ui;

import android.content.Context;
import androidx.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by thom on 15/10/3.
 */
public class ListPreferenceSummary extends ListPreference {

    private OnPreferenceClickListener mOnClickListener;

    public ListPreferenceSummary(Context context) {
        super(context);
    }

    public ListPreferenceSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence entry = getEntry();
        if (getEntries()[0].equals(entry)) {
            return entry;
        }
        return super.getSummary();
    }

    @Override
    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        mOnClickListener = onPreferenceClickListener;
    }

    @Override
    public void onClick() {
        if (mOnClickListener == null || !mOnClickListener.onPreferenceClick(this)) {
            super.onClick();
        }
    }

}
