package net.evendanan.ironhenry;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;

import net.evendanan.ironhenry.model.Categories;
import net.evendanan.ironhenry.model.Category;
import net.evendanan.ironhenry.service.CategoriesCallback;
import net.evendanan.ironhenry.service.PostsModelService;
import net.evendanan.ironhenry.service.StoryPlayerService;
import net.evendanan.ironhenry.ui.MiniPlayer;
import net.evendanan.ironhenry.ui.PostsFeedFragment;
import net.evendanan.ironhenry.utils.OnSubscribeBindService;
import net.evendanan.pushingpixels.FragmentChauffeurActivity;

import io.fabric.sdk.android.Fabric;
import rx.Observable;
import rx.Subscription;

public class MainActivity extends FragmentChauffeurActivity {

    private static final String STATE_SAVE_SELECT_NAV_ITEM_ID = "SELECT_NAV_ITEM_ID";

    private MiniPlayer mMiniPlayer;

    private Subscription mPlayerSubscription;
    @Nullable
    private StoryPlayerService.LocalBinder mPlayerBinder;
    private Subscription mPostsSubscription;
    private final CategoriesCallback mCategoriesAvailableHandler = new CategoriesCallback() {

        @Override
        public void onCategoriesAvailable(@NonNull Categories categories) {
            Menu menu = mNavigationView.getMenu();
            menu.clear();
            //adding categories
            menu.add(R.id.categories_drawer_group, 0, 0, R.string.menu_story_categories_group)
                    .setEnabled(false)
                    .setIcon(R.drawable.ic_drawer_categories);
            menu.setGroupCheckable(R.id.categories_drawer_group, true, true);
            int itemOrder = 1;
            for (Category category : categories.categories) {
                if (category.isRootCategory() && category.count > 0) {//showing only root categories
                    menu.add(R.id.categories_drawer_group, category.ID, itemOrder++, category.name + " ("+category.count+")")
                            .setCheckable(true)
                            .setChecked(false)
                            .setOnMenuItemClickListener(
                                    item -> {
                                        item.setChecked(true);
                                        menu.findItem(mSelectedNavigationItemId).setChecked(false);
                                        mSelectedNavigationItemId = category.ID;
                                        mDrawerLayout.closeDrawers();
                                        Fragment fragment = PostsFeedFragment.createPostsFeedFragment(category);
                                        addFragmentToUi(fragment, FragmentUiContext.RootFragment);
                                        return true;
                                    });
                    //TODO Add icon?
                }
            }
            menu.findItem(mSelectedNavigationItemId).setChecked(true);
            //adding static items
            //TODO Add settings fragment intent
            menu.add(R.id.settings_drawer_group, 0, itemOrder++, R.string.action_settings).setIcon(R.drawable.ic_drawer_settings);
            //TODO Add about fragment intent
            menu.add(R.id.settings_drawer_group, 0, itemOrder, R.string.drawer_item_about_app).setIcon(R.drawable.ic_drawer_about);
        }
    };
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private int mSelectedNavigationItemId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        setContentView(R.layout.activity_main);

        mMiniPlayer = new MiniPlayer(findViewById(R.id.mini_player));
        mNavigationView = Preconditions.checkNotNull((NavigationView) findViewById(R.id.side_navigation_view));
        mDrawerLayout = Preconditions.checkNotNull((DrawerLayout) findViewById(R.id.drawer_layout));

        if (null == savedInstanceState) {
            mSelectedNavigationItemId = Category.LATEST_STORIES.ID;
        } else {
            mSelectedNavigationItemId = savedInstanceState.getInt(STATE_SAVE_SELECT_NAV_ITEM_ID);
        }

        mPlayerSubscription = Observable.create(new OnSubscribeBindService(this, StoryPlayerService.class))
                .subscribe(localBinder -> {
                    mPlayerBinder = (StoryPlayerService.LocalBinder) localBinder;
                    mPlayerBinder.addListener(mMiniPlayer);
                });

        mPostsSubscription = Observable.create(new OnSubscribeBindService(this, PostsModelService.class))
                .subscribe(localBinder -> {
                    PostsModelService.LocalBinder binder = (PostsModelService.LocalBinder) localBinder;
                    binder.setCategoriesListener(mCategoriesAvailableHandler);
                });
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(mNavigationView)) {
            mDrawerLayout.closeDrawers();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerBinder != null) mPlayerBinder.removeListener(mMiniPlayer);
        mPlayerSubscription.unsubscribe();
        mPostsSubscription.unsubscribe();
    }

    @Override
    protected int getFragmentRootUiElementId() {
        return R.id.container;
    }

    @Override
    protected Fragment createRootFragmentInstance() {
        return PostsFeedFragment.createPostsFeedFragment(Category.LATEST_STORIES);
    }
}
