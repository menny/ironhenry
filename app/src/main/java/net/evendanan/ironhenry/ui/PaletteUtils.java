package net.evendanan.ironhenry.ui;

import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import java.util.List;

public class PaletteUtils {
    @Nullable
    public static Palette.Swatch getHighestPopulationSwatch(List<Palette.Swatch> swatches) {
        Palette.Swatch highestSwatch = null;
        for (Palette.Swatch swatch : swatches) {
            if (swatch != null) {
                if (highestSwatch == null || swatch.getPopulation() > highestSwatch.getPopulation()) highestSwatch = swatch;
            }
        }

        return highestSwatch;
    }
}
