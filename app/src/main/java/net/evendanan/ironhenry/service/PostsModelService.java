package net.evendanan.ironhenry.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.evendanan.ironhenry.BuildConfig;
import net.evendanan.ironhenry.model.Posts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

public class PostsModelService extends Service implements PostsModel {

    public static final String LOCAL_POSTS_CACHE_JSON = "local_posts_cache.json";

    private StorynoryRestBackend createService() {
        return new RestAdapter.Builder()
                .setEndpoint("http://www.storynory.com")
                .setConverter(new GsonConverter(Preconditions.checkNotNull(mGson)))
                .build().create(StorynoryRestBackend.class);
    }

    @NonNull
    private final Gson mGson = new GsonBuilder().create();

    @NonNull
    private final LocalBinder mLocalBinder = new LocalBinder();

    @NonNull
    private final StorynoryRestBackend mRestBackend = createService();

    @NonNull
    private Posts mPosts = new Posts();

    public PostsModelService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Observable.<Posts>create(subscriber -> {
            try {
                final InputStreamReader reader = new InputStreamReader(openFileInput(LOCAL_POSTS_CACHE_JSON));
                Posts posts = mGson.fromJson(reader, Posts.class);
                subscriber.onNext(posts);
                subscriber.onCompleted();
                reader.close();
            } catch (FileNotFoundException e) {
                //no matter.
            } catch (IOException e) {
                subscriber.onError(e);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(posts -> mPosts = posts);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public Posts getPosts(final PostsModelListener listener) {
        //read from network
        mRestBackend.getLatestPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(postsArray -> {
                    //updating local in-memory in UI thread
                    mPosts.addPosts(postsArray);
                    listener.onPostsModelChanged(mPosts);
                    return mPosts;
                })
                .observeOn(Schedulers.io())//back to IO thread
                .subscribe(posts -> {
                    try {
                        FileOutputStream outputStream = openFileOutput(LOCAL_POSTS_CACHE_JSON, BuildConfig.DEBUG ? Context.MODE_WORLD_READABLE : Context.MODE_PRIVATE);
                        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                        mGson.toJson(mPosts, writer);
                        writer.close();
                    } catch (IOException e) {
                        //no matter.
                    }
                }, throwable -> listener.onPostsModelFetchError());

        return mPosts;
    }

    public class LocalBinder extends Binder implements PostsModel {
        @Override
        public Posts getPosts(PostsModelListener listener) {
            return PostsModelService.this.getPosts(listener);
        }
    }
}
