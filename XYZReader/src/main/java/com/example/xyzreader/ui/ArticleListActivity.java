package com.example.xyzreader.ui;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlesListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.facebook.stetho.Stetho;

import timber.log.Timber;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {


    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private boolean mAnimateTransition;
   // private Interpolator mInterpolator;

    private boolean mServiceStarted;

    private Activity mThisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThisActivity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Apply activity transition
            mAnimateTransition = true;
          //  mInterpolator = AnimationUtils.loadInterpolator(mThisActivity, android.R.interpolator.linear_out_slow_in);
        } else {
            // Swap without transition
            mAnimateTransition = false;
        }

        Timber.d("Entering onCreate");

        Stetho.initializeWithDefaults(this);

        setContentView(R.layout.activity_article_list);

        mToolbar =  findViewById(R.id.collapse_toolbar);
        setSupportActionBar(mToolbar);


        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        // handle user initiated refresh requests
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // if a refresh is already in progress do not start another one
                if (!mServiceStarted) {
                    refresh();
                }
            }
        });

        // revisit- do we want the user to initiate refreshes? It seems annoying, so no.
        mSwipeRefreshLayout.setEnabled(false);


        mRecyclerView =  findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            //refresh the UI if not handling a configuration change
            refresh();
        }
    }

    private void refresh() {
        Timber.d("Starting UpdaterService");
        mServiceStarted = true;
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
        mServiceStarted = isRefreshing;
        mSwipeRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ArticlesListAdapter adapter = new ArticlesListAdapter(cursor, this,
                new ArticlesListAdapter.ArticleClickListener() {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onArticleClick(Uri uri) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if (mAnimateTransition){
                    startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(mThisActivity).toBundle());



                }else {
                    startActivity(intent);
                }
            }
        });
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        // My interpretation of the Material Design Guidelines recommends using a homogeneous
        // container for similar items. A simple recyler list view with a thumbnail should suffice
        // but I simple changed to a grid view. TODO provide reference:

        //  StaggeredGridLayoutManager sglm =
        //          new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


}
