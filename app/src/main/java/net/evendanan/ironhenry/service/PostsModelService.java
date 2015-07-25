package net.evendanan.ironhenry.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.evendanan.ironhenry.model.OfflineState;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.Posts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PostsModelService extends Service {

    private static final String TAG = PostsModelService.class.getName();

    private static final String LOCAL_POSTS_CACHE_JSON = "local_posts_cache.json";
    private static final String POST_DOWNLOAD_FILE_PREFIX = "post_download_";

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

    @NonNull
    private final Map<Post, OfflineState> mOfflineStates = new ArrayMap<>();
    @NonNull
    private final Map<Post, Subscription> mCurrentlyDownloading = new ArrayMap<>();

    public PostsModelService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Observable.<Pair<Posts, List<OfflineState>>>create(subscriber -> {
            try {
                //reading posts cache
                Posts posts = readPostsCacheFromStream(openFileInput(LOCAL_POSTS_CACHE_JSON), mGson);

                //offline state:
                //first, deleting anything related to download in the temp folder
                deleteOfflineCacheFiles(getCacheDir());
                //second, check all posts and see if they have an offline file
                ArrayList<OfflineState> offlineStates = parseOfflinePostFiles(posts, getFilesDir());

                subscriber.onNext(new Pair<Posts, List<OfflineState>>(posts, offlineStates));
                subscriber.onCompleted();
            } catch (FileNotFoundException e) {
                //no matter.
            } catch (IOException e) {
                subscriber.onError(e);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    mPosts = pair.first;
                    for (OfflineState offlineState : pair.second) {
                        mOfflineStates.put(offlineState.post, offlineState);
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Subscription subscription : mCurrentlyDownloading.values()) {
            subscription.unsubscribe();
        }
    }

    @NonNull
    private static ArrayList<OfflineState> parseOfflinePostFiles(Posts posts, File cacheDir) {
        ArrayList<OfflineState> offlineStates = new ArrayList<>();
        for (Post post : posts.posts) {
            String postFilename = getPostOfflineFilename(post);
            if (postFilename == null) continue;
            File postOfflineFile = new File(cacheDir, postFilename);
            if (postOfflineFile.exists()) {
                OfflineState offlineState = new OfflineState(post);
                offlineState.setDownloadProgress(OfflineState.PROGRESS_FULL);
                offlineStates.add(offlineState);
            }
        }
        return offlineStates;
    }

    private static void deleteOfflineCacheFiles(File cacheDir) {
        for (File tempDownloadFile : cacheDir.listFiles(pathname -> pathname.isFile() && pathname.getName().startsWith(POST_DOWNLOAD_FILE_PREFIX))) {
            if (!tempDownloadFile.delete()) {
                Log.w(TAG, "Failed to delete temp file " + tempDownloadFile);
            }
        }
    }

    protected static Posts readPostsCacheFromStream(FileInputStream postsCacheStream, Gson gson) throws IOException {
        final InputStreamReader reader = new InputStreamReader(postsCacheStream);
        Posts posts = gson.fromJson(reader, Posts.class);
        reader.close();
        return posts;
    }

    @Nullable
    private static String getPostOfflineFilename(Post post) {
        Uri mediaLink = post.extractStoryAudioLink();
        if (mediaLink == null) return null;
        return POST_DOWNLOAD_FILE_PREFIX + mediaLink.getLastPathSegment();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    private Posts fetchPosts(@NonNull final PostsFetchCallback listener) {
        //read from network
        mRestBackend.getLatestPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(postsArray -> {
                    //updating local in-memory in UI thread
                    mPosts.addPosts(postsArray);
                    listener.onPostsFetchSuccess(mPosts);
                    return mPosts;
                })
                .observeOn(Schedulers.io())//back to IO thread
                .subscribe(posts -> {
                    try {
                        FileOutputStream outputStream = openFileOutput(LOCAL_POSTS_CACHE_JSON, Context.MODE_PRIVATE);
                        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                        mGson.toJson(mPosts, writer);
                        writer.close();
                    } catch (IOException e) {
                        //no matter.
                    }
                }, throwable -> listener.onPostsFetchError());

        return mPosts;
    }

    private void setPostOfflineState(final @NonNull Post post, boolean shouldBeAvailableOffline) {
        if (shouldBeAvailableOffline) {
            if (!mCurrentlyDownloading.containsKey(post)) {
                mOfflineStates.put(post, new OfflineState(post));
                mCurrentlyDownloading.put(post,
                        Observable.<Float>create(subscriber -> {
                            subscriber.onStart();
                            try {
                                final Uri postLink = post.extractStoryAudioLink();
                                final String filename = getPostOfflineFilename(post);
                                Log.d(TAG, "Starting download post from " + post.extractStoryAudioLink());
                                if (postLink == null || filename == null)
                                    throw new IllegalStateException("Post does not have a media link! Can not download.");
                                URL url = new URL(postLink.toString());
                                URLConnection connection = url.openConnection();
                                connection.connect();

                                final int fileTotalBytesCount = connection.getContentLength();

                                Log.d(TAG, "Will download " + fileTotalBytesCount + " bytes...");

                                InputStream is = url.openStream();


                                final File cacheFile = new File(getCacheDir(), filename);
                                FileOutputStream fos = new FileOutputStream(cacheFile, false);

                                byte data[] = new byte[5 * 1024];

                                int count = 0;
                                int bytesDownloadedSoFar = 0;

                                final long PROGRESS_INTERVAL = 100;
                                long lastTimeWasUpdated = 0;
                                while ((count = is.read(data)) != -1) {
                                    bytesDownloadedSoFar += count;
                                    fos.write(data, 0, count);
                                    if (SystemClock.uptimeMillis() - lastTimeWasUpdated > PROGRESS_INTERVAL) {
                                        lastTimeWasUpdated = SystemClock.uptimeMillis();
                                        final int progressPercent = (100 * bytesDownloadedSoFar) / fileTotalBytesCount;
                                        final float progress = progressPercent / 100f;
                                        Log.d(TAG, "Downloaded " + bytesDownloadedSoFar + " (" + progress + ", " + progressPercent + "%)");
                                        //reporting progress
                                        subscriber.onNext(progress);
                                    }
                                }
                                fos.flush();
                                fos.close();
                                //copying from cache folder to persistent folder
                                if (!cacheFile.renameTo(new File(getFilesDir(), filename))) {
                                    throw new IOException("Failed to move file to permanent storage.");
                                }
                                subscriber.onCompleted();
                            } catch (Exception e) {
                                Log.w(TAG, "Error while downloading post from " + post.extractStoryAudioLink(), e);
                                subscriber.onError(e);
                            }
                        })
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .startWith(OfflineState.PROGRESS_NONE)
                                .subscribe(offlineStateProgress -> {
                                            mOfflineStates.get(post).setDownloadProgress(offlineStateProgress);
                                            mLocalBinder.onOfflineStateChanged(mOfflineStates.values());
                                        },
                                        error -> {
                                            mOfflineStates.get(post).setDownloadProgress(OfflineState.PROGRESS_ERROR);
                                            mLocalBinder.onOfflineStateChanged(mOfflineStates.values());
                                            mCurrentlyDownloading.remove(post);
                                        },
                                        () -> {
                                            mOfflineStates.get(post).setDownloadProgress(OfflineState.PROGRESS_FULL);
                                            mLocalBinder.onOfflineStateChanged(mOfflineStates.values());
                                            mCurrentlyDownloading.remove(post);
                                        })
                );
            }
        } else {
            if (mCurrentlyDownloading.containsKey(post)) {
                mCurrentlyDownloading.remove(post).unsubscribe();
            }
            Observable.<Post>create(subscriber -> {
                final String filename = getPostOfflineFilename(post);
                if (filename != null) {
                    new File(getCacheDir(), filename).delete();
                    new File(getFilesDir(), filename).delete();
                }
                subscriber.onNext(post);
                subscriber.onCompleted();
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(deletedPost -> {
                        mOfflineStates.remove(deletedPost);
                        mLocalBinder.onOfflineStateChanged(mOfflineStates.values());
                    });
        }
    }

    public class LocalBinder extends Binder implements PostsModel {
        @NonNull
        private final List<OfflineStateListener> mOfflineStateListeners = new ArrayList<>();

        @Override
        public Posts fetchPosts(@NonNull PostsFetchCallback listener) {
            return PostsModelService.this.fetchPosts(Preconditions.checkNotNull(listener));
        }

        @Override
        public void setPostOfflineState(@NonNull Post post, boolean shouldBeAvailableOffline) {
            PostsModelService.this.setPostOfflineState(post, shouldBeAvailableOffline);
        }

        private void onOfflineStateChanged(Iterable<OfflineState> offlineStates) {
            for (OfflineStateListener listener : mOfflineStateListeners)
                listener.onOfflineStateChanged(offlineStates);
        }

        @Override
        public void addOfflineStateListener(@NonNull OfflineStateListener listener) {
            mOfflineStateListeners.add(Preconditions.checkNotNull(listener));
            listener.onOfflineStateChanged(mOfflineStates.values());
        }

        @Override
        public void removeOfflineStateListener(@NonNull OfflineStateListener listener) {
            mOfflineStateListeners.remove(Preconditions.checkNotNull(listener));
        }

    }
}
