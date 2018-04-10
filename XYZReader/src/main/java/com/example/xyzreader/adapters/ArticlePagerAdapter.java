package com.example.xyzreader.adapters;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

import timber.log.Timber;


//Attribution:  https://tinyurl.com/p9kkvmn
public class ArticlePagerAdapter extends FragmentStatePagerAdapter implements ViewPager.PageTransformer {

    @Override
    public void transformPage(@NonNull View view, float position) {

        int pageWidth = view.getWidth();


        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(1);

        } else if (position <= 1) { // [-1,1]

            ImageView image = view.findViewById(R.id.article_image);
            image.setTranslationX(-position * (pageWidth / 2)); //Half the normal speed

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(1);
        }
    }

    public interface PrimaryItemListener{
        void setPrimaryItem(ViewGroup container, int position, Object object);
    }

    Cursor mCursor;
    PrimaryItemListener mPrimaryCallback;



    public ArticlePagerAdapter(FragmentManager fm, PrimaryItemListener callback) {
        super(fm);
        mPrimaryCallback = callback;

    }

    public void setCursor(Cursor cursor){
        mCursor = cursor;
    }

    @Override
    public Fragment getItem(int position) {
        Timber.d("Pager request item position: " + position);
        if (mCursor != null){
            Timber.d("Moving to position: " + position);
            mCursor.moveToPosition(position);
            Fragment fragment = ArticleDetailFragment.newInstance(mCursor.getInt(ArticleLoader.Query._ID));

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
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        mPrimaryCallback.setPrimaryItem(container, position, object);

/*
        ArticleDetailFragment selectedFragment = (ArticleDetailFragment) object;
        if (selectedFragment != null){

            Timber.d("setPrimaryItem invoked with itemID: " + selectedFragment.getItemId());
            selectedFragment.updateArticleImage();
        }else{
            Timber.d("setPrimaryItem invoked with null fragment");
        }
*/


    }
}
