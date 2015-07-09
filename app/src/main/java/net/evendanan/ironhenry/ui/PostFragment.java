package net.evendanan.ironhenry.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.MainActivity;
import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.service.StoryPlayer;
import net.evendanan.ironhenry.service.StoryPlayerListener;
import net.evendanan.pushingpixels.PassengerFragment;

import java.io.IOException;

public class PostFragment extends PassengerFragment implements StoryPlayerListener {
    private static final String ARG_KEY_POST = "ARG_KEY_POST";
    private Post mPost;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private FloatingActionButton mFab;
    private StoryPlayer mPlayer;
    private TextView mPostText;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.post_page, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCollapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setTitle(mPost.title);

        ImageView postImage = (ImageView) view.findViewById(R.id.backdrop);
        Glide.with(getActivity()).load(mPost.featuredImage.source).asBitmap().listener(new PaletteSetter()).into(postImage);

        mPostText = (TextView) view.findViewById(R.id.post);
        mPostText.setText(Html.fromHtml(mPost.htmlContent));

        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        MainActivity activity = (MainActivity) getActivity();
        activity.setPlayerStateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity activity = (MainActivity) getActivity();
        activity.setPlayerStateListener(null);
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
            Palette.Swatch topSwatch = PaletteUtils.getHighestPopulationSwatch(palette.getSwatches());
            if (topSwatch != null) {
                mCollapsingToolbar.setContentScrimColor(topSwatch.getRgb());
                mCollapsingToolbar.setExpandedTitleColor(topSwatch.getBodyTextColor());
                mCollapsingToolbar.setCollapsedTitleTextColor(topSwatch.getTitleTextColor());
                mCollapsingToolbar.setForeground(new HeroGradientDrawable(topSwatch));
            } else {
                mCollapsingToolbar.setForeground(getResources().getDrawable(R.drawable.collapsing_toolbar_foreground));
            }

            Palette.Swatch bodySwatch = palette.getLightVibrantSwatch();
            View rootView = getView();
            if (bodySwatch != null && rootView != null) {
                rootView.setBackgroundColor(bodySwatch.getRgb());
                mPostText.setTextColor(bodySwatch.getBodyTextColor());
            }
            return false;
        }
    }
}
