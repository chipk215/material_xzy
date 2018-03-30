package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlesListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

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


    private boolean mServiceStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.d("Entering onCreate");

        setContentView(R.layout.activity_article_list);

        mToolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        //final View toolbarContainerView = findViewById(R.id.toolbar_container);

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

        // revisit- do we want the user to initiate refreshes?
        mSwipeRefreshLayout.setEnabled(false);


        mRecyclerView =  findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

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
        ArticlesListAdapter adapter = new ArticlesListAdapter(cursor, this, new ArticlesListAdapter.ArticleClickListener() {
            @Override
            public void onArticleClick(Uri uri) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
      //  StaggeredGridLayoutManager sglm =
      //          new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


}
