package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.evendanan.ironhenry.model.Post;

import java.util.List;

public interface PostsModel {
    @Nullable
    List<Post> fetchPosts(@NonNull String slug, @NonNull PostsFetchCallback listener);

    void setPostOfflineState(@NonNull Post post, boolean shouldBeAvailableOffline);

    void addOfflineStateListener(@NonNull OfflineStateListener listener);

    void removeOfflineStateListener(@NonNull OfflineStateListener listener);

    void setCategoriesListener(@NonNull CategoriesCallback listener);
}
