package com.popularmovies.model.response;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import com.popularmovies.model.content.Video;


public class VideosResponse {

    @SerializedName("results")
    private List<Video> mVideos;

    @NonNull
    public List<Video> getVideos() {
        if (mVideos == null) {
            return new ArrayList<>();
        }
        return mVideos;
    }
}
