package net.evendanan.ironhenry.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.service.StoryPlayer;
import net.evendanan.ironhenry.service.StoryPlayerListener;

import java.io.IOException;

public class PostFragment extends CollapsibleFragmentBase implements StoryPlayerListener {

    private static final String ARG_KEY_POST = "ARG_KEY_POST";
    private static final String STATE_KEY_POST_SCROLL_POSITION = "STATE_KEY_POST_SCROLL_POSITION";

    private Post mPost;
    private FloatingActionButton mFab;
    private StoryPlayer mPlayer;

    public static PostFragment create(@NonNull Post post) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_KEY_POST, Preconditions.checkNotNull(post));

        PostFragment fragment = new PostFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPost = getArguments().getParcelable(ARG_KEY_POST);
    }

    @LayoutRes
    @Override
    protected int getContextLayoutResourceId() {
        return R.layout.post_page;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getCollapsingToolbar().setTitle(mPost.title);

        ImageView postImage = (ImageView) view.findViewById(R.id.backdrop);
        Glide.with(getActivity()).load(mPost.featuredImage.source).asBitmap().listener(new PaletteSetter()).into(postImage);

        TextView postText = (TextView) view.findViewById(R.id.post);
        postText.setText(Html.fromHtml(mPost.htmlContent));

        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(v -> {
            if (mPlayer == null) return;
            if (isPlayingMyPost()) {
                mFab.setImageResource(R.drawable.ic_play_audio);
                mPlayer.pauseAudio();
            } else {
                mFab.setImageResource(R.drawable.ic_pause_audio);
                try {
                    if (!mPlayer.startAudio(mPost)) {
                        mFab.setImageResource(R.drawable.ic_loading_audio);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    View rootView = getView();
                    if (rootView != null) {
                        Snackbar.make(rootView, R.string.fail_to_load_audio, Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

        final Uri audioLink = mPost.extractStoryAudioLink();
        if (audioLink == null) {
            mFab.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getMainActivity().setPlayerStateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getMainActivity().setPlayerStateListener(null);
    }

    private boolean isPlayingMyPost() {
        return mPlayer != null &&
                mPlayer.isPlaying() &&
                mPlayer.getCurrentlyPlayingPost() != null &&
                mPlayer.getCurrentlyPlayingPost().ID == mPost.ID;
    }

    @Override
    public void onPlayerStateChanged(StoryPlayer player) {
        mPlayer = player;
        if (isPlayingMyPost()) {
            mFab.setImageResource(R.drawable.ic_pause_audio);
        } else {
            mFab.setImageResource(R.drawable.ic_play_audio);
        }
    }

    private class PaletteSetter implements RequestListener<String, Bitmap> {

        @Override
        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
            Palette palette = Palette.from(resource).generate();
            //using the highest population for the toolbar, so the text will have the maximum contrast.
            final Palette.Swatch highestPopulationSwatch = PaletteUtils.getHighestPopulationSwatch(palette.getSwatches());
            setToolbarColors(highestPopulationSwatch);
            return false;
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        View view = getView();
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_POST_SCROLL_POSITION) && view != null) {
            int dy = savedInstanceState.getInt(STATE_KEY_POST_SCROLL_POSITION);
            view.findViewById(R.id.scroll_view).setScrollY(dy);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        View view = getView();
        if (view != null) {
            outState.putInt(STATE_KEY_POST_SCROLL_POSITION, view.findViewById(R.id.scroll_view).getScrollY());
        }
    }
}
