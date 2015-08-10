package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.base.Preconditions;

import net.evendanan.chauffeur.lib.FragmentChauffeurActivity;
import net.evendanan.chauffeur.lib.experiences.TransitionExperiences;
import net.evendanan.ironhenry.MainActivity;
import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.ironhenry.service.StoryPlayer;
import net.evendanan.ironhenry.service.StoryPlayerListener;

import java.io.IOException;

public class MiniPlayer implements StoryPlayerListener {

    @NonNull
    private final View mRootView;
    @NonNull
    private final ImageView mCoverArt;
    @NonNull
    private final TextView mTitle;
    @NonNull
    private final ImageView mPlayerActionButton;
    @NonNull
    private final View mSeeBarView;
    @NonNull
    private final View mPositionView;
    @NonNull
    private final View mLoadingStoryView;

    @Nullable
    private Post mCurrentPlayingPost;
    @Nullable
    private StoryPlayer mPlayer;

    public MiniPlayer(@NonNull View miniPlayerRootView) {
        mRootView = Preconditions.checkNotNull(miniPlayerRootView);
        mCoverArt = Preconditions.checkNotNull((ImageView) miniPlayerRootView.findViewById(R.id.player_cover_art));
        mTitle = Preconditions.checkNotNull((TextView) miniPlayerRootView.findViewById(R.id.player_story_title));
        mSeeBarView = Preconditions.checkNotNull(miniPlayerRootView.findViewById(R.id.seek_bar_layout));
        mPositionView = Preconditions.checkNotNull(miniPlayerRootView.findViewById(R.id.seek_bar_position_icon));
        mLoadingStoryView = Preconditions.checkNotNull(miniPlayerRootView.findViewById(R.id.seek_bar_loading));

        mPlayerActionButton = Preconditions.checkNotNull((ImageView) miniPlayerRootView.findViewById(R.id.player_action_button));
        mPlayerActionButton.setOnClickListener(v -> {
            if (mCurrentPlayingPost == null) return;
            if (mPlayer == null) return;
            if (mPlayer.isPlaying()) {
                mPlayer.pauseAudio();
            } else if (!mPlayer.isLoading()) {
                try {
                    mPlayer.startAudio(mCurrentPlayingPost);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        miniPlayerRootView.setOnClickListener(v -> {
            if (mCurrentPlayingPost == null) return;
            final Context context = v.getContext();
            Intent startPostPage = FragmentChauffeurActivity.createStartActivityIntentForAddingFragmentToUi(context, MainActivity.class, PostFragment.create(mCurrentPlayingPost), TransitionExperiences.DIALOG_EXPERIENCE_TRANSITION);
            context.startActivity(startPostPage);
        });
    }

    @Override
    public void onPlayerStateChanged(StoryPlayer player) {
        mPlayer = player;
        final boolean postChanged = mCurrentPlayingPost != player.getCurrentlyPlayingPost();
        mCurrentPlayingPost = player.getCurrentlyPlayingPost();
        if (mCurrentPlayingPost == null) {
            if (mRootView.getVisibility() != View.GONE) {
                mRootView.setVisibility(View.GONE);
            }
        } else {
            if (mRootView.getVisibility() != View.VISIBLE) {
                mRootView.setVisibility(View.VISIBLE);
            }
            if (mPlayer.isLoading()) {
                if (mLoadingStoryView.getVisibility() != View.VISIBLE) {
                    mLoadingStoryView.setVisibility(View.VISIBLE);
                    mSeeBarView.setVisibility(View.GONE);
                }
            } else {
                if (mSeeBarView.getVisibility() != View.VISIBLE) {
                    mSeeBarView.setVisibility(View.VISIBLE);
                    mLoadingStoryView.setVisibility(View.GONE);
                }
                final int positionInView = calculateSeekBarPositionInView(player);
                mPositionView.setPadding(positionInView, 0, 0, 0);
            }
            if (postChanged) {
                Glide.with(mRootView.getContext()).load(mCurrentPlayingPost.featuredImage.source).asBitmap().listener(new PaletteSetter()).into(mCoverArt);
                mTitle.setText(mCurrentPlayingPost.title);
            }
            @DrawableRes final int actionButtonResId;
            if (player.isLoading()) actionButtonResId = R.drawable.ic_loading_audio;
            else if (player.isPlaying()) actionButtonResId = R.drawable.ic_pause_audio;
            else actionButtonResId = R.drawable.ic_play_audio;
            mPlayerActionButton.setImageResource(actionButtonResId);
        }
    }

    private int calculateSeekBarPositionInView(StoryPlayer player) {
        final int duration = player.getPlayDuration();
        final int currentPosition = player.getCurrentPlayPosition();
        if (duration <= 0 || currentPosition < 0) return 0;
        final double positionFraction = Math.min(1.0, ((double) currentPosition) / ((double) duration));
        final int totalWidth = mRootView.getWidth();
        final int pointPosition = (int) (totalWidth * positionFraction);
        return Math.max(0, pointPosition);
    }


    private class PaletteSetter implements RequestListener<String, Bitmap> {

        @Override
        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
            //I want this happen on the UI thread (less flickers)
            Palette palette = Palette.from(resource).generate();
            Palette.Swatch swatch = palette.getDarkMutedSwatch();
            if (swatch != null) {
                mRootView.setBackgroundColor(swatch.getRgb());
                mTitle.setTextColor(swatch.getTitleTextColor());
            }
            return false;
        }
    }
}
