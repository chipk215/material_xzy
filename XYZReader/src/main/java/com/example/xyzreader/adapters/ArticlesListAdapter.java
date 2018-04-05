package com.example.xyzreader.adapters;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.ImageLoaderHelper;
import com.example.xyzreader.utility.DateHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import timber.log.Timber;

public class ArticlesListAdapter extends RecyclerView.Adapter<ArticlesListAdapter.ArticleViewHolder>{


    private final Context mContext;
    private Cursor mCursor;
    private final LayoutInflater mInflater;
    private final ArticleClickListener mArticleClickListener;

    public interface ArticleClickListener {
        void onArticleClick(View view,Uri uri);
    }

    public ArticlesListAdapter(Cursor cursor, Context context, ArticleClickListener listener ) {
        mCursor = cursor;
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mArticleClickListener = listener;

    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_article, parent, false);
        final ArticlesListAdapter.ArticleViewHolder vh = new ArticlesListAdapter.ArticleViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mArticleClickListener.onArticleClick(view,
                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
            }
        });
        return vh;
    }



    @Override
    public void onBindViewHolder(@NonNull ArticlesListAdapter.ArticleViewHolder holder, int position) {

        mCursor.moveToPosition(position);

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        String publishedDate =
                DateHelper.getPublishedDate(mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));
        holder.subtitleView.setText(Html.fromHtml(publishedDate
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));


        holder.thumbnailView.setImageUrl(
                mCursor.getString(ArticleLoader.Query.THUMB_URL),
                ImageLoaderHelper.getInstance(mContext).getImageLoader());
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        DynamicHeightNetworkImageView thumbnailView;
        TextView titleView;
        TextView subtitleView;

        ArticleViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView =  view.findViewById(R.id.article_title);
            subtitleView =  view.findViewById(R.id.article_subtitle);
        }
    }

}

