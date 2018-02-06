package com.popularmovies.model.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;



public class CombinedVideoReview implements Parcelable {

    private List<Video> videoList;
    private List<Review> reviewList;


    public CombinedVideoReview(List<Video> mVideo, List<Review> mReview) {
        this.videoList = mVideo;
        this.reviewList = mReview;
    }

    public CombinedVideoReview() {
    }


    private CombinedVideoReview(Parcel in) {
    }

    public static final Creator<CombinedVideoReview> CREATOR = new Creator<CombinedVideoReview>() {
        @Override
        public CombinedVideoReview createFromParcel(Parcel in) {
            return new CombinedVideoReview(in);
        }

        @Override
        public CombinedVideoReview[] newArray(int size) {
            return new CombinedVideoReview[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }

    public List<Video> getVideoList() {
        return videoList;
    }

    public void setVideoList(List<Video> videoList) {
        this.videoList = videoList;
    }

    public List<Review> getReviewList() {
        return reviewList;
    }

    public void setReviewList(List<Review> reviewList) {
        this.reviewList = reviewList;
    }
}
