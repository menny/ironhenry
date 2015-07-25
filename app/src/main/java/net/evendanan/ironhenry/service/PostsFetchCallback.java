package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.Posts;

public interface PostsFetchCallback {
    void onPostsFetchSuccess(@NonNull Posts posts);

    void onPostsFetchError();
}
