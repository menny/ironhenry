package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.Post;

import java.util.List;

public interface PostsFetchCallback {
    void onPostsFetchSuccess(@NonNull List<Post> posts);

    void onPostsFetchError();
}
