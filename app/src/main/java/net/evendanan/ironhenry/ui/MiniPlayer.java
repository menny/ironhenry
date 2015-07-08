package net.evendanan.ironhenry.ui;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

public class MiniPlayer implements StoryPlayerListener {

    @NonNull
    private final View mRootView;
    @NonNull
    private final ImageView mCoverArt;
    @NonNull
    private final TextView mTitle;
    @NonNull
    private final ImageView mPlayerActionButton;

    @Nullable
    private Post mCurrentPlayingPost;
    @Nullable
    private StoryPlayer mPlayer;

    public MiniPlayer(@NonNull View miniPlayerRootView) {
        mRootView = Preconditions.checkNotNull(miniPlayerRootView);
        mCoverArt = Preconditions.checkNotNull((ImageView)miniPlayerRootView.findViewById(R.id.player_cover_art));
        mTitle = Preconditions.checkNotNull((TextView)miniPlayerRootView.findViewById(R.id.player_story_title));
        mPlayerActionButton = Preconditions.checkNotNull((ImageView)miniPlayerRootView.findViewById(R.id.player_action_button));
        mPlayerActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPlayingPost == null) return;
                if (mPlayer == null) return;
                if (mPlayer.isPlaying()) {
                    mPlayer.pauseAudio();
                } else {
                    try {
                        mPlayer.startAudio(mCurrentPlayingPost);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onPlayerStateChanged(StoryPlayer player) {
        final boolean postChanged = mCurrentPlayingPost != player.getCurrentlyPlayingPost();
        mCurrentPlayingPost = player.getCurrentlyPlayingPost();
        mPlayer = player;
        if (mCurrentPlayingPost == null) {
            if (mRootView.getVisibility() != View.GONE) {
                mRootView.clearAnimation();
                mRootView.setVisibility(View.GONE);
                Animation slideOut = AnimationUtils.loadAnimation(mRootView.getContext(), R.anim.mini_player_slide_out);
                mRootView.startAnimation(slideOut);
            }
        } else {
            if (mRootView.getVisibility() != View.VISIBLE) {
                mRootView.clearAnimation();
                mRootView.setVisibility(View.VISIBLE);
                Animation slideOut = AnimationUtils.loadAnimation(mRootView.getContext(), R.anim.mini_player_slide_in);
                mRootView.startAnimation(slideOut);
            }
            if (postChanged) {
                Glide.with(mRootView.getContext()).load(mCurrentPlayingPost.featuredImage.source).asBitmap().listener(new PaletteSetter()).into(mCoverArt);
            }
            mTitle.setText(mCurrentPlayingPost.title);
            mPlayerActionButton.setImageResource(player.isPlaying()? R.drawable.ic_pause_audio : R.drawable.ic_play_audio);
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
            Palette.Swatch swatch = palette.getVibrantSwatch();
            if (swatch != null) {
                mRootView.setBackgroundColor(swatch.getRgb());
                mTitle.setTextColor(swatch.getTitleTextColor());
            }
            return false;
        }
    }
}
