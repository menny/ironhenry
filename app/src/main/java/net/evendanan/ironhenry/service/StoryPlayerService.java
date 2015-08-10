package net.evendanan.ironhenry.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
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
    private AudioManager mAudioManager;
    private final AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        private static final int VOLUME_NO_OP = -1;
        private boolean mHadTransientLost = false;
        private int mOriginalStreamVolume = VOLUME_NO_OP;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Crashlytics.log(Log.DEBUG, TAG, "onAudioFocusChange " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS && mMediaPlayer.isPlaying()) {
                mHadTransientLost = false;
                mOriginalStreamVolume = VOLUME_NO_OP;
                Crashlytics.log(Log.DEBUG, TAG, "Stopping playback because AUDIOFOCUS_LOSS");
                stopAudio();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && mMediaPlayer.isPlaying()) {
                Crashlytics.log(Log.DEBUG, TAG, "Pausing playback because AUDIOFOCUS_LOSS_TRANSIENT");
                mHadTransientLost = true;
                mOriginalStreamVolume = VOLUME_NO_OP;
                // Pause playback
                mMediaPlayer.pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && mMediaPlayer.isPlaying()) {
                //Lower the volume
                mHadTransientLost = true;
                mOriginalStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Crashlytics.log(Log.DEBUG, TAG, "Setting STREAM_MUSIC to 1 because AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK. Storing original volume "+mOriginalStreamVolume);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (mHadTransientLost) {
                    Crashlytics.log(Log.DEBUG, TAG, "Starting playback because AUDIOFOCUS_GAIN");
                    // Resume playback
                    mMediaPlayer.start();
                    //Raise it back to normal
                    if (mOriginalStreamVolume != VOLUME_NO_OP) {
                        Crashlytics.log(Log.DEBUG, TAG, "Setting STREAM_MUSIC to " + mOriginalStreamVolume + " because AUDIOFOCUS_GAIN");
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOriginalStreamVolume, 0);
                    }
                    mHadTransientLost = false;
                    mOriginalStreamVolume = VOLUME_NO_OP;
                }
            }
        }
    };

    public StoryPlayerService() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnPreparedListener(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        if (mPlayingSubscription != null) mPlayingSubscription.unsubscribe();
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
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

        final int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        Crashlytics.log(Log.DEBUG, TAG, "requestAudioFocus returned " + result);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            return false;
        }

        if (mCurrentlyPlayingPost == null || mCurrentlyPlayingPost.ID != post.ID) {
            if (mPlayingSubscription != null) mPlayingSubscription.unsubscribe();
            mLoading = true;
            mCurrentlyPlayingPost = post;
            mMediaPlayer.reset();
            Uri audioLink = PostsModelService.getPlayableLinkForPost(this, post);
            Crashlytics.log(Log.DEBUG, TAG, "Playing audio for post " + post.ID + " from " + audioLink);
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
            mMediaPlayer.pause();
            mLocalBinder.onPlayerStateChanged(this);
        }
    }

    @Override
    public void stopAudio() {
        stopForeground(true);
        if (mPlayingSubscription != null) mPlayingSubscription.unsubscribe();
        mLoading = false;
        mCurrentlyPlayingPost = null;
        mMediaPlayer.stop();
        mLocalBinder.onPlayerStateChanged(this);
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
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
        return mLoading ? -1 : mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPlayPosition() {
        return mLoading ? -1 : mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopAudio();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stopForeground(true);
        mLoading = false;
        mLocalBinder.onPlayerStateChanged(this);
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
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
        mPlayingSubscription = Observable.interval(16/*frame*/, TimeUnit.MILLISECONDS)
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(time -> mLocalBinder.onPlayerStateChanged(this),
                        error -> mLocalBinder.onPlayerStateChanged(this),
                        () -> mLocalBinder.onPlayerStateChanged(this));
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
