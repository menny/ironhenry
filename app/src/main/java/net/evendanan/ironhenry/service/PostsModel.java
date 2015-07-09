package net.evendanan.ironhenry.service;

import net.evendanan.ironhenry.model.Posts;

public interface PostsModel {
    Posts getPosts(PostsModelListener listener);
}
