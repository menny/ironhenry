package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.R;
import net.evendanan.ironhenry.model.Post;

public class GlideUtils {

    public static void loadPostImage(@NonNull Context context, @NonNull ImageView imageView, @Nullable String imageUri, @Nullable RequestListener<String, Bitmap> requestListener) {
        if (!TextUtils.isEmpty(imageUri)) {
            BitmapRequestBuilder<String, Bitmap> builder = Glide.with(context).load(imageUri).asBitmap().error(R.drawable.storynory);
            if (requestListener != null) {
                builder.listener(requestListener);
            }
            builder.into(imageView);
        } else {
            imageView.setImageResource(R.drawable.storynory);
        }
    }

    public static void loadPostImage(@NonNull Context context, @NonNull ImageView imageView, @NonNull Post post, @Nullable RequestListener<String, Bitmap> requestListener) {
        String imageUri = post.featuredImage == null? null : post.featuredImage.source;
        loadPostImage(context, imageView, imageUri, requestListener);
    }

    public static void loadPostImage(@NonNull Context context, @NonNull ImageView imageView, @NonNull Post post, @NonNull CollapsibleFragmentBase fragmentBase) {
        loadPostImage(context, imageView, post, new PaletteSetter(Preconditions.checkNotNull(fragmentBase)));
    }
}
