package net.evendanan.ironhenry.service;

import android.support.annotation.NonNull;

import net.evendanan.ironhenry.model.Category;
import net.evendanan.ironhenry.model.Post;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/*package*/ interface StorynoryRestBackend {
    @GET("/wp-json/posts")
    Observable<Post[]> getPostsForSlug(@NonNull @Query("filter[category_name]") String slug);


    @GET("/wp-json/taxonomies/category/terms")
    Category[] getCategories();
}
