package net.evendanan.ironhenry.ui;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;

public class PaletteSetter implements RequestListener<String, Bitmap> {

    private final WeakReference<CollapsibleFragmentBase> mFragment;

    public PaletteSetter(@NonNull CollapsibleFragmentBase fragment) {
        mFragment = new WeakReference<>(Preconditions.checkNotNull(fragment));
    }

    @Override
    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
        return false;
    }

    @Override
    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
        CollapsibleFragmentBase collapsibleFragmentBase = mFragment.get();
        if (collapsibleFragmentBase == null) return false;
        Palette palette = Palette.from(resource).generate();
        //using the highest population for the toolbar, so the text will have the maximum contrast.
        final Palette.Swatch highestPopulationSwatch = PaletteUtils.getHighestPopulationSwatch(palette.getSwatches());
        collapsibleFragmentBase.setToolbarColors(highestPopulationSwatch);
        return false;
    }
}
