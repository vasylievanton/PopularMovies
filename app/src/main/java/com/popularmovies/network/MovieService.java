package com.popularmovies.network;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.popularmovies.model.response.MoviesResponse;
import com.popularmovies.model.response.ReviewsResponse;
import com.popularmovies.model.response.VideosResponse;


public interface MovieService {

    @GET("movie/popular/")
    Observable<MoviesResponse> popularMovies();

    @GET("movie/top_rated/")
    Observable<MoviesResponse> topRatedMovies();

    @GET("movie/{movie_id}/videos")
    Observable<VideosResponse> movieTrailers(@Path("movie_id") int id);

    @GET("movie/{movie_id}/reviews")
    Observable<ReviewsResponse> movieReviews(@Path("movie_id") int id);

    @GET("search/movie")
    Observable<MoviesResponse> searchMovies(@Query("query") String query);
}
