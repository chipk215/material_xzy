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
 * Reviewer:
 *
 * I would like to have not submitted the project in its current state but I am stuck and need some
 * help.
 *
 * Here is a link to the forum where I sought advice:
 *
 * https://discussions.udacity.com/t/xyzreader-openglrenderer-error-looking-for-suggestion/659094
 *
 * Summarizing:
 * The app faults when returning to the main list activity from the detail activity. There are two
 * threads at play here the Rendering thread (15050) and my application thread 15030.
 *
 * Scenario
 *
 * Only occurs on my (only) test device not an emulator, I’m assuming the emulator does not use the
 * same OpenGL library. To trigger the error, the user must navigate to a detail screen where a
 * small block (2000 characters) of article text is displayed. A button allows the user to request
 * more text which results in the next 2k block of text being shown to the user. The user can request
 * more text which is processed in the same 2k blocks. All appears to be good with the text
 * presentation. The text processing which includes loading the article text from the database,
 * block sizing, and html formatting has been moved to a background thread in theReadBodyText async task.

 * The error occurs when the user has requested more than 1 text block and then uses the up button
 * or back button to return to the main list activity.

 * A shared item transition is implemented with the shared item being the article thumbnail
 * and article image.

 * Analysis

 * I don’t think the error is related to the shared item transition, the error does not occur if the
 * user simply navigates to different detail screens and eventually back to the main list activity
 * unless the user goes through the “read more text” scenario described above.

 * I don’t have a real device running with less than API 21 to verify the transition code is not a
 * factor and as mentioned the error doesn’t occur on an emulator irrespective of whether transitions
 * are enabled (API 21 and up) or not.

 * My best guess is the issue correlates to amending (Spannable) text to the textview outside of
 * onCreateView but this is just an hypothesis. Can anyone comment on this?

 * Perhaps I should use a separate fragment for the article text body and load the amended text only
 * in onCreateView of this fragment? This would appear to be a significant change to the design.
 *
 * I’m too inexperienced Android world to seriously consider a bug in the OpenGL library.
 *
 * I’ve spent nearly a week debugging this issue and before trudging on would like to discuss.
 *
 *
 *
 *
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
                        Timber.d("Handling return transition");
                        long startingArticleId = mTmpReenterState.getLong(EXTRA_STARTING_ARTICLE_ID);
                        long currentArticleId = mTmpReenterState.getLong(EXTRA_CURRENT_ARTICLE_ID);
                        Timber.d("start article id= %d Current article id= %d", startingArticleId,
                                currentArticleId);
                        if (startingArticleId != currentArticleId) {
                            // get the thumbnail corresponding to the current article id
                            // this only works if the requested view is visible- see note below
                            View newSharedElement = mRecyclerView.findViewWithTag(currentArticleId);
                            if (newSharedElement != null){
                                String transitionName = newSharedElement.getTransitionName();
                                Timber.d("New transition name= " + transitionName);
                                // an article with the corresponding id is in view
                                names.clear();
                                names.add(transitionName);
                                sharedElements.clear();
                                sharedElements.put(transitionName, newSharedElement);
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
            setExitSharedElementCallback(mSharedElementCallback);
        } else {
            // Swap without transition
            mAnimateTransition = false;
        }

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
        Timber.d("onActivityReenter");
        mTmpReenterState = new Bundle(data.getExtras());

        // This only works if the corresponding thumbnail view is visible on the
        // screen (or has been visible).

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
                Timber.d("Starting postponed transition in PreDraw");
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


    @SuppressLint("NewApi")
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
        mSwipeRefreshLayout.setEnabled(false);
    }

}
