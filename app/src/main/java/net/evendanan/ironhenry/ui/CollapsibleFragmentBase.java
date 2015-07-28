package net.evendanan.ironhenry.ui;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.evendanan.ironhenry.MainActivity;
import net.evendanan.ironhenry.R;
import net.evendanan.pushingpixels.PassengerFragment;

public abstract class CollapsibleFragmentBase extends PassengerFragment {

    private static final String STATE_KEY_APP_BAR_TOP_OFFSET = "PostsFeedFragment_STATE_KEY_APP_BAR_TOP_OFFSET";

    private CollapsingToolbarLayout mCollapsingToolbar;
    private AppBarLayout mAppBarLayout;

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.collapsable_page_base, container, false);
        //adding custom content
        inflater.inflate(getContextLayoutResourceId(), view, true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAppBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
        mCollapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        View view = getView();
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_APP_BAR_TOP_OFFSET) && view != null) {
            int dy = savedInstanceState.getInt(STATE_KEY_APP_BAR_TOP_OFFSET);
            CoordinatorLayout coordinator = (CoordinatorLayout) view.findViewById(R.id.coordinator);
            AppBarLayout appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
            int[] consumed = new int[2];
            behavior.onNestedPreScroll(coordinator, appBarLayout, null, 0, -dy, consumed);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);
        getMainActivity().setSupportActionBar(toolbar);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        View view = getView();
        if (view != null) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
            AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
            outState.putInt(STATE_KEY_APP_BAR_TOP_OFFSET, behavior.getTopAndBottomOffset());
        }
    }

    protected final MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    protected final CollapsingToolbarLayout getCollapsingToolbar() {
        return mCollapsingToolbar;
    }

    protected final AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    protected void setToolbarColors(Palette.Swatch swatch) {
        if (swatch != null) {
            mCollapsingToolbar.setContentScrimColor(swatch.getRgb());
            mCollapsingToolbar.setExpandedTitleColor(swatch.getBodyTextColor());
            mCollapsingToolbar.setCollapsedTitleTextColor(swatch.getTitleTextColor());
            mCollapsingToolbar.setForeground(new HeroGradientDrawable(swatch));
        } else {
            mCollapsingToolbar.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.collapsing_toolbar_foreground, getActivity().getTheme()));
        }
    }

    @LayoutRes
    protected abstract int getContextLayoutResourceId();

}
