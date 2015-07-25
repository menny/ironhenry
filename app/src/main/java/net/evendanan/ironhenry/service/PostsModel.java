package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.Posts;

public interface PostsModel {
    Posts fetchPosts(@NonNull PostsFetchCallback listener);

    void setPostOfflineState(@NonNull Post post, boolean shouldBeAvailableOffline);

    void addOfflineStateListener(@NonNull OfflineStateListener listener);

    void removeOfflineStateListener(@NonNull OfflineStateListener listener);
}
