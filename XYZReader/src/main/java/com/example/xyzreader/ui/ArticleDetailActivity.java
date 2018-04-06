package com.example.xyzreader.ui;


import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlePagerAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;

    private ViewPager mPager;

    private ArticlePagerAdapter mPagerAdapter;

    private String mArticleTitle;

    private boolean mIsReturning;

    private String getArticleTitle(){
        return mArticleTitle;
    }





    private ArticleDetailFragment mCurrentDetailsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mArticleTitle = "";

        Timber.d("Entering ArticleDetailActivity onCreate");
        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }

        setUpToolBar();
        setUpViewPager();

        setUpFAB();

    }




    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timber.d("onLoadFinished");
        mCursor = cursor;
        mPagerAdapter.setCursor(cursor);
        mPager.getAdapter().notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPager.getAdapter().notifyDataSetChanged();
    }


    @Override
    public void onBackPressed(){
        Timber.d("Back button pressed");

        int count = getFragmentManager().getBackStackEntryCount();

        super.onBackPressed();

    }


    private String formatShareMessage(Cursor cursor){
        String message = getString(R.string.share_message);
        message += System.getProperty("line.separator") + System.getProperty("line.separator");
        message += getString(R.string.title);
        message += cursor.getString(ArticleLoader.Query.TITLE);
        message += System.getProperty("line.separator") + System.getProperty("line.separator");
        message += getString(R.string.author);
        message += cursor.getString(ArticleLoader.Query.AUTHOR);
        return message;
    }


    private void setUpToolBar() {
        /*
        //attribution:  https://stackoverflow.com/a/32724422/9128441
        final CollapsingToolbarLayout collapsingToolbarLayout =  findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //TextView hiddenTitle = findViewById(R.id.hidden_title);
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {



                   // hiddenTitle.setText(getArticleTitle());
                  //  hiddenTitle.setVisibility(View.VISIBLE);
                 //   collapsingToolbarLayout.setTitle(getArticleTitle());
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle(" ");
                    isShow = false;
                  //  hiddenTitle.setVisibility(View.GONE);
                }
            }
        });

*/


        Timber.d("Setup toolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {

           // toolbar.setTitle("Title");
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Timber.d("handling tool bar nav click");
                    supportFinishAfterTransition();
                }
            });

            ActionBar actionBar = getSupportActionBar();
            // prevent app title from displaying
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setUpViewPager(){
        mPager = findViewById(R.id.article_pager);
        mPagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        float dimensionValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1, getResources().getDisplayMetrics());

        Timber.d("dimensionValue: " + dimensionValue);
        mPager.setPageMargin((int) dimensionValue);

        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

    }


    private void setUpFAB(){
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCursor != null) {
                    String message = formatShareMessage(mCursor);
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                            .setType("text/plain")
                            .setText(message)
                            .getIntent(), getString(R.string.action_share)));
                }
            }
        });
    }
}
