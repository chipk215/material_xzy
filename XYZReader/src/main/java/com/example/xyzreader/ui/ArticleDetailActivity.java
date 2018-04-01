package com.example.xyzreader.ui;


import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
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

    private long mSelectedItemId;

    private ViewPager mPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.d("Executing ArticleDetailActivity onCreate");

        setContentView(R.layout.activity_article_detail);
        setUpToolBar();
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }

        setUpViewPager();
    }

    private void setUpToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            ViewCompat.setElevation(toolbar, 0);
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    supportFinishAfterTransition();
                }
            });


            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setDisplayShowHomeEnabled(true);
            }
        }
    }

    private void setUpViewPager(){
        mPager = findViewById(R.id.article_pager);

        FragmentManager fragmentManager = getSupportFragmentManager();

        mPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                Timber.d("Pager request item position: " + position);
                if (mCursor != null){
                    int currentPosition = mCursor.getPosition();

                    mCursor.moveToPosition(position);
                    Fragment fragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
                    mCursor.moveToPosition(currentPosition);
                    return fragment;

                }
                Timber.d("Pager request article but cursor is null");
                return null;  //TODO throw an exception ?
            }

            @Override
            public int getCount() {
                return (mCursor != null) ? mCursor.getCount() : 0;
            }

            @Override
            public void setPrimaryItem(ViewGroup container, int position, Object object){
                Timber.d("setPrimaryItem invoked");
                super.setPrimaryItem(container, position, object);
                ArticleDetailFragment fragment = (ArticleDetailFragment) object;
                if (fragment != null) {
                    final ImageView articleImage = findViewById(R.id.article_image);
                    Timber.d("Displaying article bitmap");
                    mCursor.moveToPosition(position);
                    String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
                    ImageLoaderHelper.getInstance(container.getContext()).getImageLoader()
                            .get(photoUrl, new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                                    Timber.d("Article image loaded");
                                    Bitmap articleImageBitmap  = imageContainer.getBitmap();
                                    if ( articleImageBitmap!= null) {
                                       // Palette p = Palette.generate(mArticleImageBitmap, 12);
                                       // mMutedColor = p.getDarkMutedColor(0xFF333333);
                                        articleImage.setImageBitmap(articleImageBitmap);
                                       // mRootView.findViewById(R.id.meta_bar)
                                             //   .setBackgroundColor(mMutedColor);

                                    }
                                }

                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    Timber.d("Failed to obtain article image");
                                }
                            });
                }

            }
        });

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timber.d("onLoadFinished");
        mCursor = cursor;
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


}
