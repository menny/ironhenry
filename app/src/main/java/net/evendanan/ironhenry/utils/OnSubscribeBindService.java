package net.evendanan.ironhenry.utils;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class OnSubscribeBindService implements Observable.OnSubscribe<IBinder> {

    private final Context mContext;
    private final Intent mIntent;

    public OnSubscribeBindService(@NonNull Context context, @NonNull Class<? extends Service> serviceClass) {
        mContext = Preconditions.checkNotNull(context);
        mIntent = new Intent(context, Preconditions.checkNotNull(serviceClass));
    }

    @Override
    public void call(final Subscriber<? super IBinder> observer) {

        final ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                observer.onNext(binder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                observer.onNext(null);
            }
        };

        final Subscription unbindSubscription = AndroidSubscriptions.unsubscribeInUiThread(() -> {
            mContext.unbindService(connection);
            observer.onCompleted();
        });

        try {
            //by first starting the service, we are ensuring that it will not be auto-killed
            //when the activity is unbinding.
            mContext.startService(mIntent);
            final boolean result = mContext.bindService(mIntent, connection, Context.BIND_AUTO_CREATE);

            if (!result) {
                observer.onError(new Exception("Failed to bind to service using intent: " + mIntent.toString()));
            } else {
                observer.add(unbindSubscription);
            }

        } catch (Throwable error) {
            if (!observer.isUnsubscribed()) {
                observer.onError(error);
            }
        }
    }
}