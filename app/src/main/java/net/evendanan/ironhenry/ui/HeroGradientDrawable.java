package net.evendanan.ironhenry.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.graphics.Palette;

public class HeroGradientDrawable extends GradientDrawable {
    public HeroGradientDrawable(Palette.Swatch swatch) {
        super(Orientation.TOP_BOTTOM, getColorsFromSwatch(swatch));
    }

    private static int[] getColorsFromSwatch(Palette.Swatch swatch) {
        final int rgb = swatch.getRgb();
        return new int[]{Color.argb(0, 0, 0, 0), Color.argb(0xA0, Color.red(rgb), Color.green(rgb), Color.blue(rgb))};
    }
}
