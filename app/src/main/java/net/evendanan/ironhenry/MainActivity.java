package net.evendanan.ironhenry;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.crashlytics.android.Crashlytics;

import net.evendanan.ironhenry.model.Posts;
import net.evendanan.ironhenry.service.PostsModel;
import net.evendanan.ironhenry.service.PostsModelListener;
import net.evendanan.ironhenry.service.PostsModelService;
import net.evendanan.ironhenry.service.StoryPlayerListener;
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
    @Nullable
    private StoryPlayerService.LocalBinder mPlayerBinder;
    @Nullable
    private StoryPlayerListener mTempPlayerStateListener;

    @Nullable
    private PostsModel mPostsBinder;
    @Nullable
    private PostsModelListener mTempPostsModelListener;

    private final PostsModel mPostsModelProxy = new PostsModel() {
        @Override
        public Posts getPosts(PostsModelListener listener) {
            if (mPostsBinder != null) {
                return mPostsBinder.getPosts(listener);
            } else {
                mTempPostsModelListener = listener;
                return new Posts();/*returning an empty model, which will be updated once the service will be created*/
            }
        }
    };
    private Subscription mPlayerSubscription;
    private Subscription mPostsModelSubscription;


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

        mPlayerSubscription = Observable.create(new OnSubscribeBindService(this, StoryPlayerService.class))
                .subscribe(localBinder -> {
                    mPlayerBinder = (StoryPlayerService.LocalBinder) localBinder;
                    if (mTempPlayerStateListener != null)
                        mPlayerBinder.addListener(mTempPlayerStateListener);
                    mPlayerBinder.addListener(mMiniPlayer);
                });

        mPostsModelSubscription = Observable.create(new OnSubscribeBindService(this, PostsModelService.class))
                .subscribe(localBinder -> {
                    mPostsBinder = (PostsModel) localBinder;
                    if (mTempPostsModelListener != null) {
                        mTempPostsModelListener.onPostsModelChanged(mPostsBinder.getPosts(mTempPostsModelListener));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerBinder != null) mPlayerBinder.removeListener(mMiniPlayer);
        mPlayerSubscription.unsubscribe();
        mPostsModelSubscription.unsubscribe();
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

    public PostsModel getPostsModel() {
        return mPostsModelProxy;
    }
}
