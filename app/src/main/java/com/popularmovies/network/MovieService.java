package com.popularmovies.network;

import retrofit2.http.GET;
import retrofit2.http.Path;
import com.popularmovies.model.response.MoviesResponse;
import com.popularmovies.model.response.ReviewsResponse;
import rx.Observable;


public interface MovieService {

    @GET("popular/")
    Observable<MoviesResponse> popularMovies();

    @GET("{movie_id}/reviews")
    Observable<ReviewsResponse> moviesReview(@Path("movie_id") int id);

}
