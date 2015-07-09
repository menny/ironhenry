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

import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.Posts;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class PostsModelService extends Service implements PostsModel {


    private static StorynoryRestBackend createService() {
        Gson gson = new GsonBuilder().create();

        return new RestAdapter.Builder()
                .setEndpoint("http://www.storynory.com")
                .setConverter(new GsonConverter(gson))
                .build().create(StorynoryRestBackend.class);
    }

    @NonNull
    private final LocalBinder mLocalBinder = new LocalBinder();

    @NonNull
    private final StorynoryRestBackend mRestBackend = createService();

    @NonNull
    private Posts mPosts = new Posts();

    public PostsModelService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public Posts getPosts(final PostsModelListener listener) {
        mRestBackend.getLatestPosts(new Callback<Post[]>() {
            @Override
            public void success(Post[] posts, Response response) {
                mPosts.addPosts(posts);
                listener.onPostsModelChanged(mPosts);
            }

            @Override
            public void failure(RetrofitError error) {
                listener.onPostsModelFetchError();
            }
        });
        return mPosts;
    }

    public static void bind(Context context, ServiceConnection serviceConnection) {
        Intent service = new Intent(Preconditions.checkNotNull(context), PostsModelService.class);
        //by first starting the service, we are ensuring that it will not be auto-killed
        //when the activity is unbinding.
        context.startService(service);
        context.bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public class LocalBinder extends Binder implements PostsModel {
        @Override
        public Posts getPosts(PostsModelListener listener) {
            return PostsModelService.this.getPosts(listener);
        }
    }
}
