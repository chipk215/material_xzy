package com.example.xyzreader;


import android.app.Application;

import timber.log.Timber;

public class XyzReader extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

        Timber.plant(new ReleaseTree());

    }


    private static class ReleaseTree extends Timber.Tree{

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {

            // ignore for now
            return;
        }
    }
}
