package com.popularmovies.model.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class Review extends RealmObject implements Parcelable{

    @PrimaryKey
    private int mId;

    @SerializedName("author")
    private String mAuthor;

    @SerializedName("content")
    private String mContent;


    protected Review(Parcel in) {
        mId = in.readInt();
        mAuthor = in.readString();
        mContent = in.readString();
    }

    public static final Creator<Review> CREATOR = new Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };

    public Review() {
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mId);
        parcel.writeString(mAuthor);
        parcel.writeString(mContent);
    }
}
