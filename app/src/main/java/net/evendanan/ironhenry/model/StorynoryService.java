package net.evendanan.ironhenry.model;

import retrofit.Callback;
import retrofit.http.GET;

public interface StorynoryService {
    @GET("/wp-json/posts?filter[category_name]=latest-stories")
    void getLastestPosts(Callback<Post[]> callback);
}
