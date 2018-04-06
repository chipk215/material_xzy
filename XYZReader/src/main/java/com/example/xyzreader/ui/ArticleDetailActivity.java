package com.example.xyzreader.ui;


import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlePagerAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import timber.log.Timber;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;
    private ViewPager mPager;
    private ArticlePagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.postponeEnterTransition(this);

        Timber.d("Entering ArticleDetailActivity onCreate");
        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());

            }
        }

        setUpViewPager();
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


    private void setUpViewPager(){
        mPager = findViewById(R.id.article_pager);
        mPagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        float dimensionValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1, getResources().getDisplayMetrics());

        Timber.d("dimensionValue: " + dimensionValue);
        mPager.setPageMargin((int) dimensionValue);

        //TODO Fix this
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

    }

}



/* Revisit one page adapter is working */
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