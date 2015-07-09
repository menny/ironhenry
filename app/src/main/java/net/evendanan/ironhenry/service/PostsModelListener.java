package net.evendanan.ironhenry.service;

import net.evendanan.ironhenry.model.Posts;

public interface PostsModelListener {
    void onPostsModelChanged(Posts posts);
    void onPostsModelFetchError();
}
