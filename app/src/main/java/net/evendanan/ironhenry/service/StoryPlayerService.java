package net.evendanan.ironhenry.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.BuildConfig;
import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class StoryPlayerService extends Service implements StoryPlayer, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = StoryPlayerService.class.getName();
    private final LocalBinder mLocalBinder = new LocalBinder();
    private final MediaPlayer mMediaPlayer;
    @Nullable
    private Post mCurrentlyPlayingPost;
    private boolean mLoading;

    @Nullable
    private Subscription mPlayingSubscription;

    public StoryPlayerService() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnPreparedListener(this);
    }

    @Override
    public void onDestroy() {
        if (mPlayingSubscription != null ) mPlayingSubscription.unsubscribe();
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mLocalBinder.onPlayerStateChanged(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    public boolean startAudio(@NonNull Post post) throws IOException {
        Preconditions.checkNotNull(post);

        PackageManager packageManager = getPackageManager();
        Intent restartAppIntent = packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification_playing)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(post.title)
                .setTicker(post.title)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, restartAppIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        startForeground(R.id.story_playing, builder.build());

        if (mCurrentlyPlayingPost == null || mCurrentlyPlayingPost.ID != post.ID) {
            if (mPlayingSubscription != null ) mPlayingSubscription.unsubscribe();
            mLoading = true;
            mCurrentlyPlayingPost = post;
            mMediaPlayer.reset();
            Uri audioLink = PostsModelService.getPlayableLinkForPost(this, post);
            Log.d(TAG, "Playing audio for post " + post.ID + " from " + audioLink);
            mMediaPlayer.setDataSource(this, audioLink);
            mMediaPlayer.prepareAsync();
            mLocalBinder.onPlayerStateChanged(this);
            return false;
        } else if (!mLoading) {
            mMediaPlayer.start();
            mLocalBinder.onPlayerStateChanged(this);
            return true;
        } else {
            return false;
        }
    }

    public void pauseAudio() {
        if (mMediaPlayer.isPlaying() && !mLoading) {
            stopForeground(true);
            mMediaPlayer.pause();
            mLocalBinder.onPlayerStateChanged(this);
        }
    }

    public void seek(int seconds) {
        if (mMediaPlayer.isPlaying() && !mLoading) {
            int seekPosition = Math.max(0, mMediaPlayer.getCurrentPosition() + seconds * 1000);
            seekPosition = Math.min(mMediaPlayer.getDuration(), seekPosition);
            mMediaPlayer.seekTo(seekPosition);
        }
    }

    @Override
    public boolean isLoading() {
        return mLoading;
    }

    public boolean isPlaying() {
        return (!mLoading) && mMediaPlayer.isPlaying();
    }

    @Nullable
    public Post getCurrentlyPlayingPost() {
        return mCurrentlyPlayingPost;
    }

    @Override
    public int getPlayDuration() {
        return mLoading? -1 : mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPlayPosition() {
        return mLoading? -1 : mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopForeground(true);
        if (mPlayingSubscription != null ) mPlayingSubscription.unsubscribe();
        mLoading = false;
        mCurrentlyPlayingPost = null;
        mLocalBinder.onPlayerStateChanged(this);

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stopForeground(true);
        mLoading = false;
        mLocalBinder.onPlayerStateChanged(this);
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mLocalBinder.onPlayerStateChanged(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mLoading = false;
        mp.start();
        mPlayingSubscription = Observable.interval(16, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(time -> {
                    mLocalBinder.onPlayerStateChanged(this);
                });
    }

    public class LocalBinder extends Binder {

        @NonNull
        private final List<StoryPlayerListener> mStoryPlayerListeners = new ArrayList<>();

        private void onPlayerStateChanged(StoryPlayer player) {
            for (StoryPlayerListener listener : mStoryPlayerListeners)
                listener.onPlayerStateChanged(player);
        }

        public void addListener(@NonNull StoryPlayerListener listener) {
            mStoryPlayerListeners.add(Preconditions.checkNotNull(listener));
            listener.onPlayerStateChanged(StoryPlayerService.this);
        }

        public void removeListener(@NonNull StoryPlayerListener listener) {
            mStoryPlayerListeners.remove(Preconditions.checkNotNull(listener));
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }
}
