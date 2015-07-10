package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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

import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;
import net.evendanan.pushingpixels.FragmentChauffeurActivity;

import java.util.ArrayList;
import java.util.List;

public class FeedItemsAdapter extends RecyclerView.Adapter<FeedItemsAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @NonNull
        public final CardView cardView;
        @NonNull
        public final ImageView imageView;
        @NonNull
        public final TextView title;
        @NonNull
        public final TextView excerpt;

        @Nullable
        private Post mData;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) Preconditions.checkNotNull(itemView.findViewById(R.id.post_card_view));
            imageView = (ImageView) Preconditions.checkNotNull(itemView.findViewById(R.id.post_image));
            title = (TextView) Preconditions.checkNotNull(itemView.findViewById(R.id.post_title));
            excerpt = (TextView) Preconditions.checkNotNull(itemView.findViewById(R.id.post_excerpt));
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mData == null) return;
            PostFragment fragment = PostFragment.create(mData);

            FragmentChauffeurActivity activity = (FragmentChauffeurActivity) mContext;
            activity.addFragmentToUi(fragment, FragmentChauffeurActivity.FragmentUiContext.ExpandedItem, itemView);
        }

        public void setData(@NonNull Post data) {
            mData = Preconditions.checkNotNull(data);
        }
    }

    @NonNull
    private final List<Post> mPostsList;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;

    private final int mDefaultPrimaryColor;
    private final int mDefaultSecondaryColor;
    private final int mDefaultBackgroundColor;

    public FeedItemsAdapter(Context context) {
        mContext = Preconditions.checkNotNull(context);
        mLayoutInflater = LayoutInflater.from(context);
        mPostsList = new ArrayList<>();
        mDefaultPrimaryColor = context.getResources().getColor(R.color.primary_text);
        mDefaultSecondaryColor = context.getResources().getColor(R.color.secondary_text);
        mDefaultBackgroundColor = context.getResources().getColor(R.color.primary);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = mLayoutInflater.inflate(R.layout.posts_feed_item, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post item = mPostsList.get(position);
        Glide.with(mContext).load(item.featuredImage.source).asBitmap().listener(new PaletteSetter(holder)).into(holder.imageView);
        holder.title.setText(item.title);
        holder.excerpt.setText(Html.fromHtml(item.excerpt));
        holder.setData(item);
    }

    @Override
    public int getItemCount() {
        return mPostsList.size();
    }

    private class PaletteSetter implements RequestListener<String, Bitmap> {
        private final ViewHolder mViewHolder;

        public PaletteSetter(ViewHolder viewHolder) {
            mViewHolder = viewHolder;
        }

        @Override
        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
            Palette palette = Palette.from(resource).generate();
            Palette.Swatch swatch = palette.getLightVibrantSwatch();
            if (swatch != null) {
                mViewHolder.title.setTextColor(swatch.getTitleTextColor());
                mViewHolder.excerpt.setTextColor(swatch.getBodyTextColor());
                mViewHolder.cardView.setCardBackgroundColor(swatch.getRgb());
            } else {
                mViewHolder.title.setTextColor(mDefaultPrimaryColor);
                mViewHolder.excerpt.setTextColor(mDefaultSecondaryColor);
                mViewHolder.cardView.setCardBackgroundColor(mDefaultBackgroundColor);
            }
            return false;
        }
    }

    public void addPosts(List<Post> posts) {
        if (mPostsList.size() == 0) {
            //this is the case where we updating the initial set
            mPostsList.addAll(posts);
            notifyItemRangeInserted(0, posts.size());
        } else {
            //now we are adding new items
            for (Post post : posts) {
                final int insertPosition = findInsertPositionForPost(post);
                if (insertPosition >= 0) {
                    mPostsList.add(insertPosition, post);
                    notifyItemInserted(insertPosition);
                }
            }
        }
    }

    private int findInsertPositionForPost(Post newPost) {
        int insertPosition = 0;
        for (Post existingPost : mPostsList) {
            if (existingPost.ID == newPost.ID) return -1;
            if (existingPost.ID < newPost.ID) return insertPosition;
            insertPosition++;
        }
        return insertPosition;
    }

}
