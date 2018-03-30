package com.example.xyzreader.adapters;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public interface ArticleClickListener {
        void onArticleClick(Uri uri);
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
                mArticleClickListener.onArticleClick(ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
            }
        });
        return vh;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex);

            Timber.i("passing today's date");

            return new Date();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ArticlesListAdapter.ArticleViewHolder holder, int position) {

        mCursor.moveToPosition(position);

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }
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

