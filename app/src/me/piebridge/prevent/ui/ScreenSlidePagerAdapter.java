package me.piebridge.prevent.ui;



import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * Created by thom on 2016/10/26.
 */
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

    private final PreventFragment[] mFragments;

    public ScreenSlidePagerAdapter(FragmentManager fm) {
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mFragments = new PreventFragment[2];
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new Applications();
        } else if (position == 1) {
            return new SystemApplications();
        } else {
            return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        PreventFragment fragment = (PreventFragment) super.instantiateItem(container, position);
        mFragments[position] = fragment;
        return fragment;
    }

    /*@Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new Applications();
        } else if (position == 1) {
            return new PreventList();
        } else {
            return null;
        }
    }
     */
    public PreventFragment getFragment(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return 2;
    }


}