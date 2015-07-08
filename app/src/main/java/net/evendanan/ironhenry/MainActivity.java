package net.evendanan.ironhenry;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.crashlytics.android.Crashlytics;

import net.evendanan.ironhenry.service.PlayerPlayerService;
import net.evendanan.ironhenry.service.StoryPlayerListener;
import net.evendanan.ironhenry.ui.MiniPlayer;
import net.evendanan.ironhenry.ui.PostsFeedFragment;
import net.evendanan.pushingpixels.FragmentChauffeurActivity;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends FragmentChauffeurActivity {

    private MiniPlayer mMiniPlayer;
    @Nullable
    private PlayerPlayerService.LocalBinder mPlayerBinder;
    @Nullable
    private StoryPlayerListener mTempPlayerStateListener;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayerBinder = (PlayerPlayerService.LocalBinder) service;
            if (mTempPlayerStateListener != null)
                mPlayerBinder.addListener(mTempPlayerStateListener);
            mPlayerBinder.addListener(mMiniPlayer);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerBinder = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
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
        PlayerPlayerService.bind(this, mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerBinder != null) mPlayerBinder.removeListener(mMiniPlayer);
        unbindService(mServiceConnection);
    }

    @Override
    protected int getFragmentRootUiElementId() {
        return R.id.container;
    }

    @Override
    protected Fragment createRootFragmentInstance() {
        return new PostsFeedFragment();
    }

    public void setPlayerStateListener(StoryPlayerListener listener) {
        if (mPlayerBinder == null) {
            mTempPlayerStateListener = listener;
        } else {
            if (listener == null && mTempPlayerStateListener != null) {
                mPlayerBinder.removeListener(mTempPlayerStateListener);
                mTempPlayerStateListener = null;
            } else if (listener != null) {
                mPlayerBinder.addListener(listener);
                mTempPlayerStateListener = listener;
            }
        }
    }
}
