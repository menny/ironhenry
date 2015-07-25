package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.Posts;
import net.evendanan.ironhenry.service.PostsModel;
import net.evendanan.ironhenry.service.PostsFetchCallback;
import net.evendanan.ironhenry.service.PostsModelService;
import net.evendanan.ironhenry.utils.OnSubscribeBindService;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;

public class PostsFeedFragment extends CollapsibleFragmentBase {

    private static final String STATE_KEY_LOADED_POSTS_LIST = "STATE_KEY_LOADED_POSTS_LIST";

    private final PostsFetchCallback mOnFeedAvailable = new PostsFetchCallback() {
        @Override
        public void onPostsFetchSuccess(@NonNull Posts posts) {
            mSwipeRefreshLayout.setRefreshing(false);
            setPosts(posts.posts);
        }

        @Override
        public void onPostsFetchError() {
            View parent = getView();
            if (parent == null) return;
            mSwipeRefreshLayout.setRefreshing(false);
            Snackbar.make(parent, "Can't fetch stories.", Snackbar.LENGTH_LONG)
                    .setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mPostsModel != null) {
                                mSwipeRefreshLayout.setRefreshing(true);
                                mPostsModel.fetchPosts(mOnFeedAvailable);
                            }
                        }
                    })
                    .show(); // Don’t forget to show!

        }
    };

    private Subscription mModelSubscription;

    @Nullable
    private PostsModel mPostsModel;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FeedItemsAdapter mFeedItemsAdapter;

    private void setPosts(@NonNull List<Post> posts) {
        Context context = getActivity();
        if (context == null) return;
        mFeedItemsAdapter.addPosts(posts);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView feedsList = (RecyclerView) view.findViewById(R.id.feed_items);
        feedsList.addItemDecoration(new MarginDecoration(getActivity()));
        feedsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFeedItemsAdapter = new FeedItemsAdapter(getActivity());
        feedsList.setAdapter(mFeedItemsAdapter);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_LOADED_POSTS_LIST)) {
            List<Post> loadedPosts = savedInstanceState.getParcelableArrayList(STATE_KEY_LOADED_POSTS_LIST);
            mFeedItemsAdapter.addPosts(loadedPosts);
        }
        getCollapsingToolbar().setTitle(getText(R.string.latest_stories_feed_title));

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (mPostsModel != null) mPostsModel.fetchPosts(mOnFeedAvailable);
        });

        mModelSubscription = Observable.create(new OnSubscribeBindService(getActivity(), PostsModelService.class))
                .subscribe(localBinder -> {
                    mPostsModel = (PostsModelService.LocalBinder) localBinder;
                    Posts postsModel = mPostsModel.fetchPosts(mOnFeedAvailable);
                    if (postsModel != null && postsModel.posts.size() > 0) {
                        setPosts(postsModel.posts);
                    } else {
                        /* Workaround ahead: https://code.google.com/p/android/issues/detail?id=77712*/
                        TypedValue typed_value = new TypedValue();
                        getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
                        mSwipeRefreshLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));
                        /*End of workaround*/
                        mSwipeRefreshLayout.setRefreshing(true);
                    }
                });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mFeedItemsAdapter != null) {
            List<Post> loadedPosts = mFeedItemsAdapter.getPostsList();
            if (loadedPosts != null && loadedPosts.size() > 0) {
                outState.putParcelableArrayList(STATE_KEY_LOADED_POSTS_LIST, new ArrayList<>(loadedPosts));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mModelSubscription.unsubscribe();
    }

    @LayoutRes
    @Override
    protected int getContextLayoutResourceId() {
        return R.layout.fragment_feed;
    }
}
