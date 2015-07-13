package net.evendanan.ironhenry;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.crashlytics.android.Crashlytics;

import net.evendanan.ironhenry.service.StoryPlayerService;
import net.evendanan.ironhenry.ui.MiniPlayer;
import net.evendanan.ironhenry.ui.PostsFeedFragment;
import net.evendanan.ironhenry.utils.OnSubscribeBindService;
import net.evendanan.pushingpixels.FragmentChauffeurActivity;

import io.fabric.sdk.android.Fabric;
import rx.Observable;
import rx.Subscription;

public class MainActivity extends FragmentChauffeurActivity {

    private MiniPlayer mMiniPlayer;

    private Subscription mPlayerSubscription;
    @Nullable
    private StoryPlayerService.LocalBinder mPlayerBinder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        setContentView(R.layout.activity_main);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        */

        mMiniPlayer = new MiniPlayer(findViewById(R.id.mini_player));

        mPlayerSubscription = Observable.create(new OnSubscribeBindService(this, StoryPlayerService.class))
                .subscribe(localBinder -> {
                    mPlayerBinder = (StoryPlayerService.LocalBinder) localBinder;
                    mPlayerBinder.addListener(mMiniPlayer);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerBinder != null) mPlayerBinder.removeListener(mMiniPlayer);
        mPlayerSubscription.unsubscribe();
    }

    @Override
    protected int getFragmentRootUiElementId() {
        return R.id.container;
    }

    @Override
    protected Fragment createRootFragmentInstance() {
        return new PostsFeedFragment();
    }
}
