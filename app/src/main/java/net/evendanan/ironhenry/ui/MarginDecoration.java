package net.evendanan.ironhenry.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.evendanan.ironhenry.R;

public class MarginDecoration extends RecyclerView.ItemDecoration {
    private final int mMargin;

    public MarginDecoration(Context context) {
        mMargin = context.getResources().getDimensionPixelSize(R.dimen.cards_padding);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(mMargin, mMargin, mMargin, mMargin);
    }
}
