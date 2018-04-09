package com.example.xyzreader.ui;


import android.annotation.SuppressLint;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticlePagerAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_ARTICLE_ID;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_ARTICLE_ID;


/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        ArticlePagerAdapter.PrimaryItemListener {

    private Cursor mCursor;
    private long mStartArticleId;
    private ViewPager mPager;
    private ArticlePagerAdapter mPagerAdapter;

    private int mStartPosition;
    private ArticleDetailFragment mCurrentDetailsFragment;
    private boolean mIsReturning;

    private final SharedElementCallback mSharedElementCallback;


    public ArticleDetailActivity() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Remap thumbnail view if user changed views using pager adapter
            mSharedElementCallback = new SharedElementCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mIsReturning) {
                        ImageView sharedElement = mCurrentDetailsFragment.getArticleImageView();
                        if (sharedElement == null) {
                            // If shared element is null, then it has been scrolled off screen and
                            // no longer visible. In this case we cancel the shared element transition by
                            // removing the shared element from the shared elements map.
                            names.clear();
                            sharedElements.clear();
                        } else if (mStartArticleId != mCurrentDetailsFragment.getArticleId()) {
                            // If the user has swiped to a different ViewPager page, then we need to
                            // remove the old shared element and replace it with the new shared element
                            // that should be transitioned instead.
                            names.clear();
                            names.add(sharedElement.getTransitionName());
                            sharedElements.clear();
                            sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                        }
                    }
                }
            };
        }else{
            mSharedElementCallback = null;
        }
    }


    @Override
    public void supportFinishAfterTransition() {
        Timber.d("executing supportFinishAfterTransition");
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_ARTICLE_ID, mStartArticleId);
        data.putExtra(EXTRA_CURRENT_ARTICLE_ID, mCurrentDetailsFragment.getArticleId());
        setResult(RESULT_OK, data);
        super.supportFinishAfterTransition();
    }

/*
    @Override
    public void onBackPressed() {
        Timber.d("executing onBackPressed");
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
            supportFinishAfterTransition();
        } else {
            getSupportFragmentManager().popBackStack();
        }

    }
*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.postponeEnterTransition(this);
        mIsReturning = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setEnterSharedElementCallback(mSharedElementCallback);
        }

        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        mStartArticleId = 0;
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartArticleId = ItemsContract.Items.getItemId(getIntent().getData());
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
        if (mStartArticleId > 0) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartArticleId) {
                    mStartPosition = mCursor.getPosition();
                    mPager.setCurrentItem(mStartPosition, false);

                    break;
                }
                mCursor.moveToNext();
            }
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        mCurrentDetailsFragment = (ArticleDetailFragment) object;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPager.getAdapter().notifyDataSetChanged();
    }


    private void setUpViewPager(){
        mPager = findViewById(R.id.article_pager);
        mPagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager(), this);
        mPager.setAdapter(mPagerAdapter);
        float dimensionValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1, getResources().getDisplayMetrics());
        Timber.d("dimensionValue: " + dimensionValue);
        mPager.setPageMargin((int) dimensionValue);
        //what is this?
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
    }


}

