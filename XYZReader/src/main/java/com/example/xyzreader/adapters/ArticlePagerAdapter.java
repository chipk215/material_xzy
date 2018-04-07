package com.example.xyzreader.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

import timber.log.Timber;

public class ArticlePagerAdapter extends FragmentStatePagerAdapter {

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
