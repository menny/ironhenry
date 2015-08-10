package net.evendanan.ironhenry;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class IronHenryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());
        Crashlytics.setString("BUILD_TYPE", BuildConfig.BUILD_TYPE);
    }
}
