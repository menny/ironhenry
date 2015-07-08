package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.StorynoryService;
import net.evendanan.ironhenry.model.StorynoryServiceFactory;
import net.evendanan.pushingpixels.PassengerFragment;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PostsFeedFragment extends PassengerFragment {

    private static final String SAVED_STATE_KEY_POSTS = "SAVED_STATE_KEY_POSTS";
    private StorynoryService mService;
    @Nullable
    private Post[] mPosts;

    private final Callback<Post[]> mOnFeedAvailable = new Callback<Post[]>() {
        @Override
        public void success(Post[] posts, Response response) {
            setPosts(posts);
        }

        @Override
        public void failure(RetrofitError error) {
            View parent = getView();
            if (parent == null) return;
            Snackbar.make(parent, "Can't fetch stories.", Snackbar.LENGTH_LONG)
                    .setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mService.getLastestPosts(mOnFeedAvailable);
                        }
                    })
                    .show(); // Don’t forget to show!
        }
    };

    private void setPosts(@NonNull Post[] posts) {
        Context context = getActivity();
        if (context == null) return;
        mPosts = Preconditions.checkNotNull(posts);
        FeedItemsAdapter adapter = new FeedItemsAdapter(context, posts);
        mFeedsList.setAdapter(adapter);
        mFeedsList.setVisibility(View.VISIBLE);
    }

    private RecyclerView mFeedsList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mService = StorynoryServiceFactory.createService();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFeedsList = (RecyclerView) view.findViewById(R.id.feed_items);
        mFeedsList.addItemDecoration(new MarginDecoration(getActivity()));
        mFeedsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getText(R.string.lastest_stories_feed_title));

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_STATE_KEY_POSTS)) {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(SAVED_STATE_KEY_POSTS);
            if (parcelables != null && parcelables.length > 0) {
                mPosts = new Post[parcelables.length];
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(parcelables, 0, mPosts, 0, parcelables.length);
            }
        }
        if (mPosts == null) {
            mService.getLastestPosts(mOnFeedAvailable);
        } else {
            setPosts(mPosts);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPosts != null) {
            outState.putParcelableArray(SAVED_STATE_KEY_POSTS, mPosts);
        }
    }
}
