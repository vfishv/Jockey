package com.marverenic.music.ui;

import android.app.Activity;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//import com.crashlytics.android.Crashlytics;
import com.trello.rxlifecycle.components.support.RxFragment;

import timber.log.Timber;

public abstract class BaseFragment extends RxFragment {

    /**
     * If this fragment is attached to a {@link BaseActivity}, then this callback will be triggered
     * when the back button is pressed. If multiple BaseFragments are visible in a single activity,
     * there is no guarantee on which fragment will get the callback first.
     * @return Whether or not this event was consumed. If false, the activity will handle the event.
     * @see Activity#onBackPressed()
     */
    protected boolean onBackPressed() {
        return false;
    }

    protected void setActivitySupportActionBar(Toolbar toolbar) {
        Activity hostingActivity = getActivity();
        if (hostingActivity instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) hostingActivity;
            activity.setSupportActionBar(toolbar);
        } else {
            Timber.w("Hosting activity is not an AppCompatActivity. Toolbar will not be bound.");
        }
    }

    @Nullable
    protected ActionBar getActivitySupportActionBar() {
        Activity hostingActivity = getActivity();
        if (hostingActivity instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) hostingActivity;
            return activity.getSupportActionBar();
        } else {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Crashlytics.setString("last_foreground_fragment", getClass().getName());
    }
}
