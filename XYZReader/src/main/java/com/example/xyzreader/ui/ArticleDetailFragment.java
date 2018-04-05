package com.example.xyzreader.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.utility.DateHelper;

import timber.log.Timber;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";

    private static final int TEXT_BLOCK_SIZE = 2000;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mCharactersConsumed;


    private String mPhotoURL;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            Timber.d("Detail Fragment onCreate... itemId= " + mItemId);
        }

        mCharactersConsumed = 0;

    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Timber.d("Entering onCreateView");
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        return mRootView;
    }




    private void bindViews() {

        /*  What is taking so long? ... not image loading but text formatting.
        SimpleDateFormat date =
                new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
        String logDate = date.format(new Date());
        Debug.startMethodTracing("sample-" + logDate);
        */

        Timber.d("Entering bindViews for mItemId: " + mItemId);
        if (mRootView == null) {
            Timber.d("Exiting bindViews due to null mRootView");
            return;
        }



        Timber.d("Obtaining view ids");
        TextView titleView =  mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        final TextView bodyView =  mRootView.findViewById(R.id.article_body);
        final Button readMore = mRootView.findViewById(R.id.read_more);

        // get body text
        Timber.d("Request body text");
        final String bodyText = mCursor.getString(ArticleLoader.Query.BODY);
        final int articleLength = bodyText.length();


        readMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String newText;
                if ((articleLength - mCharactersConsumed) > TEXT_BLOCK_SIZE){
                    int startIndex = mCharactersConsumed;
                    newText = bodyText.substring(startIndex,
                            startIndex + TEXT_BLOCK_SIZE);
                    int lastSpaceIndex = newText.lastIndexOf(" ",newText.length());

                    newText = newText.substring(0, lastSpaceIndex);
                    mCharactersConsumed+= newText.length();

                }else{
                    newText = bodyText.substring(mCharactersConsumed);
                    readMore.setVisibility(View.INVISIBLE);
                }


                int nextSpace = newText.indexOf(' ',1);
                if (nextSpace == -1){
                    nextSpace = 1;
                }

                appendColoredText(bodyView,newText.substring(0,nextSpace),Color.MAGENTA);


                String remainder = "&nbsp" + newText.substring(nextSpace) ;
                bodyView.append(Html.fromHtml(remainder));

            }
        });


        final LinearLayout metaBar = mRootView.findViewById(R.id.meta_bar);

        final ImageView articleImage = getActivity().findViewById(R.id.article_image);

        // Seems to be appropriate typeface for articles: http://www.1001fonts.com/rosario-font.html
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {

            // retrieve title
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            // retrieve published date
            String publishedDate =
                    DateHelper.getPublishedDate(
                            mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));

            bylineView.setText(Html.fromHtml(publishedDate + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));


            Spanned htmlBody;
            if (articleLength > TEXT_BLOCK_SIZE){
                String bodyString = mCursor.getString(ArticleLoader.Query.BODY).substring(0,TEXT_BLOCK_SIZE);
                int lastSpaceIndex = bodyString.lastIndexOf(" ",TEXT_BLOCK_SIZE-1);
                mCharactersConsumed = lastSpaceIndex;

                htmlBody = Html.fromHtml(bodyString.substring(0,lastSpaceIndex) );
            }else{
                htmlBody = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).substring(0,TEXT_BLOCK_SIZE));
                // use invisible to keep bottom padding
                readMore.setVisibility(View.INVISIBLE);
                readMore.setEnabled(false);
            }

            bodyView.setText(htmlBody);

            Timber.d("Body text returned");

            // get photo
            mPhotoURL = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            Timber.d("Retrieve photo url: " + mPhotoURL);
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mPhotoURL, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Timber.d("Article image loaded");
                            Bitmap articleImageBitmap  = imageContainer.getBitmap();
                            if ( articleImageBitmap!= null) {
                                Palette p = Palette.from(articleImageBitmap).maximumColorCount(12).generate();
                                 int mutedMetaBarColor = p.getDarkMutedColor(getResources()
                                         .getColor(R.color.meta_bar_default_muted_color));
                                 articleImage.setImageBitmap(articleImageBitmap);
                                 metaBar.setBackgroundColor(mutedMetaBarColor);
                                Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                                toolbar.setBackgroundColor(mutedMetaBarColor);


                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Timber.d("Failed to obtain article image");
                        }
                    });


        } else {
            Timber.d("mCursor null ... exiting bindViews");
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }

        Timber.d("Exiting bindviews for mItemId: " + mItemId);
      /*  Debug.stopMethodTracing();  */
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Timber.d("Entering onLoadFinished");
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        Timber.d("Cursor now valid in detail fragment");
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Timber.e( "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Timber.d("onLoaderReset invoked");
        mCursor = null;
        bindViews();
    }

    @Override
    public void onResume(){
        super.onResume();
        Timber.d("Detail Fragment on Resume.. itemId = " + mItemId);
    }

    private static void appendColoredText(TextView tv, String text, int color) {
        int start = tv.getText().length();
        tv.append(text);
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }

}
