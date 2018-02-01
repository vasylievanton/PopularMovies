package com.popularmovies.model.content;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class Review extends RealmObject {

    @PrimaryKey
    @SerializedName("id")
    private int mId;

    @SerializedName("author")
    private String mAuthor;

    @SerializedName("content")
    private String mContent;


    @NonNull
    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(@NonNull String author) {
        mAuthor = author;
    }

    @NonNull
    public String getContent() {
        return mContent;
    }

    public void setContent(@NonNull String content) {
        mContent = content;
    }

    public int getmId() {
        return mId;
    }

    public void setmId(int mId) {
        this.mId = mId;
    }
}
