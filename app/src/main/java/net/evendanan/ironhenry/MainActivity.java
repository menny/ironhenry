package net.evendanan.ironhenry;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.SubMenu;

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
            SubMenu categoriesMenu = menu.addSubMenu(R.string.menu_story_categories_group);
            for (Category category : categories.categories) {
                if (category.isRootCategory()) {//showing only root categories
                    categoriesMenu.add(1, category.ID, category.ID, category.name).setOnMenuItemClickListener(
                            item -> {
                                mDrawerLayout.closeDrawers();
                                Fragment fragment = PostsFeedFragment.createPostsFeedFragment(category.slug);
                                addFragmentToUi(fragment, FragmentUiContext.RootFragment);
                                return true;
                            });
                    //TODO Add icon?
                }
            }
            //adding static items
            //TODO Add settings fragment intent
            menu.add(R.string.action_settings);
            //TODO Add about fragment intent
            menu.add(R.string.drawer_item_about_app);
        }
    };
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;


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
        return PostsFeedFragment.createPostsFeedFragment("latest-stories");
    }
}
