package net.evendanan.ironhenry;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public class IronHenryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());
        RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
            @Override
            public void handleError(Throwable e) {
                Crashlytics.log("RxJava error occurred: "+e);
                Crashlytics.logException(e);
            }
        });
        Crashlytics.setString("BUILD_TYPE", BuildConfig.BUILD_TYPE);
    }
}
