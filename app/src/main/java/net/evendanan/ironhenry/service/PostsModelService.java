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
import android.util.SparseArray;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.evendanan.ironhenry.model.Categories;
import net.evendanan.ironhenry.model.Category;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PostsModelService extends Service {

    private static final String TAG = PostsModelService.class.getName();

    private static final String LOCAL_CATEGORIES_CACHE_JSON = "local_categories_cache.json";
    private static final String LOCAL_POSTS_CACHE_JSON = "local_posts_cache.json";
    private static final String POST_DOWNLOAD_FILE_PREFIX = "post_download_";

    private static <T> T readCacheFromStream(FileInputStream cacheStream, Gson gson, Class<T> clazz) throws IOException {
        final InputStreamReader reader = new InputStreamReader(cacheStream);
        T cachedModel = gson.fromJson(reader, clazz);
        reader.close();
        return cachedModel;
    }

    private StorynoryRestBackend createService() {
        return new RestAdapter.Builder()
                .setEndpoint("http://www.storynory.com")
                .setConverter(new GsonConverter(Preconditions.checkNotNull(mGson)))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build()
                .create(StorynoryRestBackend.class);
    }

    @NonNull
    private final Gson mGson = new GsonBuilder().create();

    @NonNull
    private final LocalBinder mLocalBinder = new LocalBinder();

    @NonNull
    private final StorynoryRestBackend mRestBackend = createService();

    @NonNull
    private Posts mPosts = new Posts();

    private Observable<Categories> mCategoriesCache;

    @NonNull
    private final Map<Post, OfflineState> mOfflineStates = new ArrayMap<>();
    @NonNull
    private final Map<Post, Subscription> mCurrentlyDownloading = new ArrayMap<>();

    public PostsModelService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mCategoriesCache = Observable.<Categories>create(subscriber -> {
            SparseArray<Category> sparseArray = new SparseArray<>();
            try {
                //reading categories cache - so we'll have something quickly for the client
                Categories cachedCategories = readCacheFromStream(openFileInput(LOCAL_CATEGORIES_CACHE_JSON), mGson, Categories.class);
                subscriber.onNext(cachedCategories);
                for (Category category : cachedCategories.categories) {
                    sparseArray.put(category.ID, category);
                }
            } catch (IOException e) {
                //no matter.
                Log.w(TAG, "Failed to load categories from cache. That can happen.", e);
            }
            //now, we can check for new data in the backend
            try {
                Category[] backendCategories = mRestBackend.getCategories();
                for (Category category : backendCategories) {
                    sparseArray.put(category.ID, category);
                }
            } catch (RetrofitError error) {
                //no matter.. I guess.
                Log.w(TAG, "Failed to load categories from backend... That's a shame.", error);
            }

            //making sure we have at least one category (in case we failed loading from cache or from network
            if (sparseArray.size() == 0) {
                sparseArray.put(1, new Category(1, "Latest stories", "latest-stories", "", "http://www.storynory.com/category/latest-stories/", 0, null));
            }
            final Comparator<? super Category> categoriesIdComparator = (lhs, rhs) -> lhs.ID - rhs.ID;
            Categories categories = new Categories();
            for (int arrayKeyIndex = 0; arrayKeyIndex < sparseArray.size(); arrayKeyIndex++) {
                categories.categories.add(sparseArray.valueAt(arrayKeyIndex));
            }
            Collections.sort(categories.categories, categoriesIdComparator);
            subscriber.onNext(categories);

            //writing to cache
            try {
                FileOutputStream outputStream = openFileOutput(LOCAL_CATEGORIES_CACHE_JSON, Context.MODE_PRIVATE);
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                mGson.toJson(categories, writer);
                writer.close();
            } catch (IOException e) {
                //no matter.
            }
            subscriber.onCompleted();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .cache(1);

        Observable.<Pair<Posts, List<OfflineState>>>create(subscriber -> {
            try {
                //reading posts cache
                Posts posts = readCacheFromStream(openFileInput(LOCAL_POSTS_CACHE_JSON), mGson, Posts.class);

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
        for (Map.Entry<String, List<Post>> entry : posts.postsMap.entrySet()) {
            for (Post post : entry.getValue()) {
                String postFilename = getPostOfflineFilename(post);
                if (postFilename == null) continue;
                File postOfflineFile = new File(cacheDir, postFilename);
                if (postOfflineFile.exists()) {
                    OfflineState offlineState = new OfflineState(post);
                    offlineState.setDownloadProgress(OfflineState.PROGRESS_FULL);
                    offlineStates.add(offlineState);
                }
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

    @Nullable
    private List<Post> fetchPosts(final @NonNull String slug, @NonNull final PostsFetchCallback listener) {
        //read from network
        mRestBackend.getPostsForSlug(Preconditions.checkNotNull(slug))
                .subscribeOn(Schedulers.io())
                .map(postsArray -> {
                    try {
                        FileOutputStream outputStream = openFileOutput(LOCAL_POSTS_CACHE_JSON, Context.MODE_PRIVATE);
                        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                        mGson.toJson(mPosts, writer);
                        writer.close();
                    } catch (IOException e) {
                        //no matter.
                    }
                    return postsArray;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(postsArray -> {
                    List<Post> posts = mPosts.addPosts(slug, postsArray);
                    listener.onPostsFetchSuccess(posts);
                }, throwable -> listener.onPostsFetchError());

        return mPosts.postsMap.get(slug);
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

    public static Uri getPlayableLinkForPost(Context context, Post post) {
        //checking if the post has offline data
        String filename = Preconditions.checkNotNull(getPostOfflineFilename(post));
        File offlineData = new File(context.getFilesDir(), filename);
        if (offlineData.exists()) return Uri.fromFile(offlineData);
        else return post.extractStoryAudioLink();
    }

    public class LocalBinder extends Binder implements PostsModel {
        @NonNull
        private final List<OfflineStateListener> mOfflineStateListeners = new ArrayList<>();

        @Override
        public List<Post> fetchPosts(@NonNull String slug, @NonNull PostsFetchCallback listener) {
            return PostsModelService.this.fetchPosts(
                    Preconditions.checkNotNull(slug),
                    Preconditions.checkNotNull(listener));
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

        @Override
        public void setCategoriesListener(@NonNull CategoriesCallback listener) {
            mCategoriesCache.subscribe(listener::onCategoriesAvailable);
        }

    }
}
