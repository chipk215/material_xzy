package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
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
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.tasks.ReadBodyText;
import com.example.xyzreader.utility.DateHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ReadBodyText.BodyTextCallback{

    public static final String ARG_ITEM_ID = "item_id";
    private static final String SAVE_BYTES_READ_KEY = "save_bytes_read_key";

    // Paged article text size
    public static final int TEXT_BLOCK_SIZE = 2000;

    private Unbinder mUnbinder;

    private Cursor mCursor;
    private long mItemId;

    private View mRootView;
    private int mCharactersConsumed;
    private int mInitialReadSize;
    private String mPhotoURL;

    @BindView(R.id.article_body) TextView mBodyView;

    @BindView(R.id.read_more) Button mReadMore;

    @BindView(R.id.article_title) TextView mTitleView;

    @BindView(R.id.article_byline) TextView mByLineView ;

    @BindView(R.id.meta_bar) LinearLayout mMetaBar;

    @BindView(R.id.article_image) ImageView mArticleImage;

    @BindView(R.id.toolbar ) Toolbar mToolBar;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {}

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

        if (savedInstanceState != null){
            mInitialReadSize = savedInstanceState.getInt(SAVE_BYTES_READ_KEY);
            mItemId = savedInstanceState.getLong(ARG_ITEM_ID);
        }else if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            Timber.d("Detail Fragment onCreate... itemId= %s", mItemId);
            mInitialReadSize = TEXT_BLOCK_SIZE;
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
        mUnbinder = ButterKnife.bind(this, mRootView);
        setUpToolBar();
        setUpFAB();
        return mRootView;
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Timber.d("Entering onCreateLoader");
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Timber.d("Entering onLoadFinished");
        if (!isAdded()) {
            if (cursor != null) {
                Timber.d("closing cursor, fragment not added");
                cursor.close();
            }
            return;
        }

        Timber.d("onLoadFinished Cursor valid in detail fragment with itemID= %s", mItemId);
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Timber.e( "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mArticleImage.setTransitionName(mCursor.getString(ArticleLoader.Query._ID));
                getActivity().startPostponedEnterTransition();

            bindViews();
        }


    }


    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        Timber.d("onLoaderReset invoked");
        mCursor = null;
        bindViews();
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        Timber.d("Saving state data on configuration change");
        savedInstanceState.putInt(SAVE_BYTES_READ_KEY,mCharactersConsumed);
        savedInstanceState.putLong(ARG_ITEM_ID, mItemId);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onDestroyView(){

        mUnbinder.unbind();
        super.onDestroyView();
    }


    private void setUpToolBar() {
        if (mToolBar != null) {

            ((AppCompatActivity)getActivity()).setSupportActionBar(mToolBar);
            mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Timber.d("handling tool bar nav click");
                  //  ((ViewGroup)mBodyView.getParent()).removeView(mBodyView);
                    getActivity().supportFinishAfterTransition();
                }
            });

            ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            // prevent app title from displaying
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    private void setUpFAB(){
        mRootView.findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mCursor != null) {
                    String message = formatShareMessage(mCursor);
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText(message)
                            .getIntent(), getString(R.string.action_share)));
                }
            }
        });
    }


    private void bindViews() {

        /*  What is taking so long? ... not image loading but text formatting.
        SimpleDateFormat date =
                new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
        String logDate = date.format(new Date());
        Debug.startMethodTracing("sample-" + logDate);
        */

        Timber.d("Entering bindViews for mItemId: %s", mItemId);
        if (mRootView == null) {
            Timber.d("Exiting bindViews due to null mRootView");
            return;
        }
        mBodyView.setText("");

        mByLineView.setMovementMethod(new LinkMovementMethod());

        Timber.d("Request body text");

        final ArticleDetailFragment thisFragment = this;
        mReadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getContext(), "Click", Toast.LENGTH_SHORT).show();
                ReadBodyText reader = new ReadBodyText(mCharactersConsumed,thisFragment);
                reader.execute(mCursor);
            }
        });

        // Seems to be appropriate typeface for articles: http://www.1001fonts.com/rosario-font.html
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            // retrieve title
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            // retrieve published date
            String publishedDate =
                    DateHelper.getPublishedDate(
                            mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));

            mByLineView.setText(Html.fromHtml(publishedDate + " by <font color='#ffffff'>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</font>"));


            ReadBodyText reader = new ReadBodyText(0,mInitialReadSize,this);
            reader.execute(mCursor);

            // get photo
            mPhotoURL = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            Timber.d("Retrieve photo url: %s", mPhotoURL);
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mPhotoURL, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Timber.d("Article image loaded");
                            final Bitmap articleImageBitmap  = imageContainer.getBitmap();
                            if ( articleImageBitmap!= null) {

                                Palette.from(articleImageBitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(@NonNull Palette palette) {
                                        int mutedMetaBarColor = palette.getDarkMutedColor(getResources()
                                                .getColor(R.color.meta_bar_default_muted_color));
                                        mArticleImage.setImageBitmap(articleImageBitmap);
                                        mMetaBar.setBackgroundColor(mutedMetaBarColor);
                                        mToolBar.setBackgroundColor(mutedMetaBarColor);
                                    }
                                });
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
            mTitleView.setText("N/A");
            mByLineView.setText("N/A" );
            mBodyView.setText("N/A");
        }

        Timber.d("Exiting bindviews for mItemId: %s", mItemId);
        /*  Debug.stopMethodTracing();  */
    }



    private static void appendColoredText(TextView tv, String text, int color) {
        int start = tv.getText().length();
        tv.append(text);
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }



    // Combine the article title and author information
    private  String formatShareMessage(Cursor cursor){
        String message = getString(R.string.share_message);
        message += System.getProperty("line.separator") + System.getProperty("line.separator");
        message += getString(R.string.title);
        message += cursor.getString(ArticleLoader.Query.TITLE);
        message += System.getProperty("line.separator") + System.getProperty("line.separator");
        message += getString(R.string.author);
        message += cursor.getString(ArticleLoader.Query.AUTHOR);
        return message;
    }


    public ImageView getArticleImageView(){
        return mArticleImage;
    }

    public long getArticleId(){
        return mItemId;
    }

    @Override
    public void ReadBodyText(Spanned htmlBody, int charactersRead,
                             boolean lastBlockRead, String continuationWord) {
        if (continuationWord != null){
            // Color the first word to help the reader locate the start of new text
            appendColoredText(mBodyView,continuationWord,
                    getResources().getColor(R.color.first_wordColor));
        }

        mBodyView.append(htmlBody);
        mCharactersConsumed+=charactersRead;
        if (lastBlockRead){
            mReadMore.setVisibility(View.INVISIBLE);
        }
    }
}
