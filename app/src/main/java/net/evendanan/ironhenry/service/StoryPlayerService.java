package net.evendanan.ironhenry.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.BuildConfig;
import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoryPlayerService extends Service implements StoryPlayer, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private final LocalBinder mLocalBinder = new LocalBinder();
    private final MediaPlayer mMediaPlayer;
    @Nullable
    private Post mCurrentlyPlayingPost;

    @NonNull
    private final List<StoryPlayerListener> mStoryPlayerListeners = new ArrayList<>();

    public StoryPlayerService() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnPreparedListener(this);
    }

    @Override
    public void onDestroy() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        for (StoryPlayerListener listener : mStoryPlayerListeners)
            listener.onPlayerStateChanged(this);
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
            mCurrentlyPlayingPost = post;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(this, post.extractStoryAudioLink());
            mMediaPlayer.prepareAsync();
            for (StoryPlayerListener listener : mStoryPlayerListeners)
                listener.onPlayerStateChanged(this);
            return false;
        } else {
            mMediaPlayer.start();
            for (StoryPlayerListener listener : mStoryPlayerListeners)
                listener.onPlayerStateChanged(this);
            return true;
        }
    }

    public void pauseAudio() {
        if (mMediaPlayer.isPlaying()) {
            stopForeground(true);
            mMediaPlayer.pause();
            for (StoryPlayerListener listener : mStoryPlayerListeners)
                listener.onPlayerStateChanged(this);
        }
    }

    public void seek(int seconds) {
        if (mMediaPlayer.isPlaying()) {
            int seekPosition = Math.max(0, mMediaPlayer.getCurrentPosition() + seconds * 1000);
            seekPosition = Math.min(mMediaPlayer.getDuration(), seekPosition);
            mMediaPlayer.seekTo(seekPosition);
        }
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Nullable
    public Post getCurrentlyPlayingPost() {
        return mCurrentlyPlayingPost;
    }

    @Override
    public int getPlayDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPlayPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mCurrentlyPlayingPost = null;
        for (StoryPlayerListener listener : mStoryPlayerListeners)
            listener.onPlayerStateChanged(this);

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        for (StoryPlayerListener listener : mStoryPlayerListeners)
            listener.onPlayerStateChanged(this);
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        stopForeground(true);
        for (StoryPlayerListener listener : mStoryPlayerListeners)
            listener.onPlayerStateChanged(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        for (StoryPlayerListener listener : mStoryPlayerListeners)
            listener.onPlayerStateChanged(this);
    }

    public class LocalBinder extends Binder {

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
