package net.evendanan.ironhenry.service;

import net.evendanan.ironhenry.model.Post;

import retrofit.Callback;
import retrofit.http.GET;
import rx.Observable;

/*package*/ interface StorynoryRestBackend {
    @GET("/wp-json/posts?filter[category_name]=latest-stories")
    Observable<Post[]> getLatestPosts();
}
