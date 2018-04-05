package com.example.xyzreader.adapters;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

import timber.log.Timber;

public class ArticlePagerAdapter extends FragmentStatePagerAdapter {
    Cursor mCursor;
    public ArticlePagerAdapter(FragmentManager fm) {
        super(fm);

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
            Fragment fragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));

            return fragment;

        }
        Timber.d("Pager request article but cursor is null");
        return null;  //TODO throw an exception ?
    }

    @Override
    public int getCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }
}