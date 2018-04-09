package com.example.xyzreader.tasks;

import android.database.Cursor;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

public class ReadBodyText extends AsyncTask<Cursor, Void, Spanned> {

    public interface BodyTextCallback{
        void ReadBodyText(Spanned htmlBody, int charactersRead,
                          boolean lastBlockRead, String continuationWord);
    }
    private int mStartIndex;
    private int mCharactersConsumed;
    private BodyTextCallback mCallback;

    private boolean mLastBlockRead;
    String mContinuationWord = null;


    public ReadBodyText(int startIndex,  BodyTextCallback callback) {
        mStartIndex = startIndex;
        mCallback = callback;

        mLastBlockRead = false;
    }


    @Override
    protected Spanned doInBackground(Cursor... cursors) {
        Spanned htmlBody;


        String articleText =cursors[0].getString(ArticleLoader.Query.BODY);


        int articleLength = articleText.length();
        int charactersToRead;
        int remainingCharacters = articleLength - mStartIndex;
        String bodyString;

        if (remainingCharacters > ArticleDetailFragment.TEXT_BLOCK_SIZE){

            // read a block sized chunk

            charactersToRead = ArticleDetailFragment.TEXT_BLOCK_SIZE;
            bodyString = articleText.substring(mStartIndex,mStartIndex+charactersToRead);

            // Don't split a word, find the last occurring space in the new text
            int newTextLength = bodyString.length();
            int lastSpaceIndex = bodyString.lastIndexOf(" ",newTextLength);
            if (lastSpaceIndex == -1){
                lastSpaceIndex = newTextLength;
            }

            // truncate text to end of last word
            bodyString = bodyString.substring(0, lastSpaceIndex);
            mCharactersConsumed = bodyString.length();


        }else{
            //last block
            bodyString = articleText.substring(mStartIndex);
            mCharactersConsumed = bodyString.length();
            mLastBlockRead = true;
        }


        if (mStartIndex > 0){
            // extract the first word for coloring
            // determine end of first word in new text
            int endOfFirstWordIndex = bodyString.indexOf(' ',1);
            if (endOfFirstWordIndex == -1){
                endOfFirstWordIndex = 1;
            }
            mContinuationWord = bodyString.substring(0, endOfFirstWordIndex);
            bodyString = "&nbsp" + bodyString.substring(endOfFirstWordIndex) ;

        }


        htmlBody = Html.fromHtml(bodyString );
        return htmlBody;
    }

    @Override
    protected void onPostExecute(Spanned spanned) {
       mCallback.ReadBodyText(spanned, mCharactersConsumed, mLastBlockRead, mContinuationWord);
    }
}
