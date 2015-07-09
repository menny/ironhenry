package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.os.Bundle;
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

import net.evendanan.ironhenry.MainActivity;
import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.model.Posts;
import net.evendanan.ironhenry.service.PostsModel;
import net.evendanan.ironhenry.service.PostsModelListener;
import net.evendanan.pushingpixels.PassengerFragment;

import java.util.List;

public class PostsFeedFragment extends PassengerFragment {

    private final PostsModelListener mOnFeedAvailable = new PostsModelListener() {
        @Override
        public void onPostsModelChanged(Posts posts) {
            setPosts(posts.posts);
        }

        @Override
        public void onPostsModelFetchError() {
            View parent = getView();
            if (parent == null) return;
            Snackbar.make(parent, "Can't fetch stories.", Snackbar.LENGTH_LONG)
                    .setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mPostsModel.getPosts(mOnFeedAvailable);
                        }
                    })
                    .show(); // Don’t forget to show!

        }
    };

    private PostsModel mPostsModel;

    private void setPosts(@NonNull List<Post> posts) {
        Context context = getActivity();
        if (context == null) return;
        FeedItemsAdapter adapter = new FeedItemsAdapter(context, posts);
        mFeedsList.setAdapter(adapter);
    }

    private RecyclerView mFeedsList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity mainActivity = Preconditions.checkNotNull((MainActivity) getActivity());
        mPostsModel = mainActivity.getPostsModel();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFeedsList = (RecyclerView) view.findViewById(R.id.feed_items);
        mFeedsList.addItemDecoration(new MarginDecoration(getActivity()));
        mFeedsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getText(R.string.lastest_stories_feed_title));

        Posts postsModel = mPostsModel.getPosts(mOnFeedAvailable);
        if (postsModel != null && postsModel.posts.size() > 0) {
            setPosts(postsModel.posts);
        }
    }
}
