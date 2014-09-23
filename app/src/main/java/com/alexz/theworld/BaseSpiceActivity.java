package com.alexz.theworld;

import android.app.Activity;
import android.app.Dialog;

import com.octo.android.robospice.SpiceManager;

/**
 * Base class for all activity on project
 */
public abstract class BaseSpiceActivity extends Activity {


    private final String TAG = BaseSpiceActivity.class.getName();
    private SpiceManager spiceManager = new SpiceManager(TheWorldSpiceService.class);
    private Dialog loadingDialog;

    public SpiceManager getSpiceManager() {
        return spiceManager;
    }


    @Override
    public void onStart() {
        spiceManager.start(this);
        super.onStart();
    }


    @Override
    public void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }
}

