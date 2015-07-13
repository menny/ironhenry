package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.evendanan.ironhenry.model.Post;

import java.io.IOException;

public interface StoryPlayer {
    boolean startAudio(@NonNull Post post) throws IOException;

    void pauseAudio();

    void seek(int seconds);

    boolean isPlaying();

    @Nullable
    Post getCurrentlyPlayingPost();

    int getPlayDuration();

    int getCurrentPlayPosition();

    boolean isLoading();
}
