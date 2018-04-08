package com.example.xyzreader.ui;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlesListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    static final String EXTRA_STARTING_ARTICLE_ID = "extra_starting_article_id";
    static final String EXTRA_CURRENT_ARTICLE_ID = "extra_current_article_id";


    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Bundle mTmpReenterState;

    private boolean mAnimateTransition;

    private Activity mThisActivity;

    private final SharedElementCallback mSharedElementCallback;

    public ArticleListActivity(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Attribution https://github.com/alexjlockwood/adp-activity-transitions
            //
            // I pretty much followed this example to learn how to adjust the shared
            // item return transition when the invoked activity changes the the
            // original referenced view via a pager adapter.

            mSharedElementCallback = new SharedElementCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mTmpReenterState != null) {
                        long startingArticleId = mTmpReenterState.getLong(EXTRA_STARTING_ARTICLE_ID);
                        long currentArticleId = mTmpReenterState.getLong(EXTRA_CURRENT_ARTICLE_ID);
                        if (startingArticleId != currentArticleId) {
                            // get the thumbnail corresponding to the current article id
                            // this only works if the requested view is visible- see note below
                            View newSharedElement = mRecyclerView.findViewWithTag(currentArticleId);
                            if (newSharedElement != null){
                                // an article with the corresponding id is in view
                                names.clear();
                                names.add(newSharedElement.getTransitionName());
                                sharedElements.clear();
                                sharedElements.put(newSharedElement.getTransitionName(), newSharedElement);
                            }
                        }
                        mTmpReenterState = null;
                    }else{
                        //sharedElements.put()
                    }
                }
            };
        }else{
            mSharedElementCallback = null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_list);

        mThisActivity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Apply activity transition
            mAnimateTransition = true;
        } else {
            // Swap without transition
            mAnimateTransition = false;
        }

        //Stetho.initializeWithDefaults(this);

        mToolbar =  findViewById(R.id.collapse_toolbar);
        setSupportActionBar(mToolbar);

        initRefreshLayout();

        initRecyclerView();

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            //refresh the UI if not handling a configuration change
            startUpdateService();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());

        // This only works if the corresponding thumbnail view is visible on the
        // screen (or has been visible) after returning to ArticleListActivity and refreshing from the
        // cloud's XYZ article server.  The refresh could mean the corresponding
        // article is no longer available (it was removed from the server) or
        // the corresponding thumbnail view is not bound to the recycler view
        // because it is offscreen.

        // We need a more general solution that matches an article id with any corresponding
        // thumbnail view and then scrolls the corresponding view to be on-screen if it exists.

        // I tried multiple approaches (unsuccessfully) which seemed to be beyond the scope
        // of this project.


        //Attribution: https://github.com/alexjlockwood/adp-activity-transitions
        postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    private void startUpdateService() {
        Timber.d("Starting UpdaterService");
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("onStart executing");
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.d("onStop executing");
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy executing");
    }


    // Broadcast receiver which listens to UpdateService article refresh update state changes
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // UpdateService broadcast prior to starting a fetch and again when fetch is complete
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                boolean isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                Timber.d("Received Broadcast Action State Change Intent: " + isRefreshing);
                updateRefreshingUI(isRefreshing);
            }
        }
    };

    private void updateRefreshingUI(boolean isRefreshing) {
        Timber.d("SwipeRefreshLayout.setRefreshing: " + isRefreshing);
        mSwipeRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timber.d("onLoadFinished invoked.. new adapter") ;
        ArticlesListAdapter adapter = new ArticlesListAdapter(cursor, this,
                new ArticlesListAdapter.ArticleClickListener() {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onArticleClick(View view, Uri uri) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                DynamicHeightNetworkImageView imageView = view.findViewById(R.id.thumbnail);
                if (mAnimateTransition){
                    Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(
                            mThisActivity,
                            imageView,
                            imageView.getTransitionName()).toBundle();

                    startActivity(intent, bundle);

                }else {
                    startActivity(intent);
                }
            }
        });
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


    private void initRecyclerView(){
        mRecyclerView =  findViewById(R.id.recycler_view);
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        // My interpretation of the Material Design Guidelines recommends using a homogeneous
        // container for similar items. A simple recycler list view with a thumbnail should suffice
        // but I simple changed to a grid view.
        // reference: https://material.io/guidelines/components/cards.html#cards-usage
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));
    }


    private void initRefreshLayout(){
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

}
