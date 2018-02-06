package com.popularmovies.model.response;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import com.popularmovies.model.content.Review;


public class ReviewsResponse {

    @SerializedName("id")
    private int mId;

    @SerializedName("results")
    private List<Review> mReviews;

    @NonNull
    public List<Review> getReviews() {
        if (mReviews == null) {
            mReviews = new ArrayList<>();
        }
        for (Review m : mReviews) {
            m.setmId(mId);
        }
        return mReviews;
    }
}
