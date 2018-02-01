package com.popularmovies.screen.details;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import com.popularmovies.R;
import com.popularmovies.model.content.Movie;
import com.popularmovies.model.content.Review;
import com.popularmovies.model.content.Video;
import com.popularmovies.model.response.ReviewsResponse;
import com.popularmovies.network.ApiFactory;
import com.popularmovies.screen.loading.LoadingDialog;
import com.popularmovies.screen.loading.LoadingView;
import com.popularmovies.utils.Images;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String MAXIMUM_RATING = "10";

    public static final String IMAGE = "image";
    public static final String EXTRA_MOVIE = "extraMovie";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbar;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    TextView mTitleTextView;

    @BindView(R.id.overview)
    TextView mOverviewTextView;

    @BindView(R.id.rating)
    TextView mRatingTextView;

    @BindView(R.id.review)
    TextView mReviewTextView;

    @Nullable
    private Subscription mMoviesReviewSubscription;

    public static void navigate(@NonNull AppCompatActivity activity, @NonNull View transitionImage,
                                @NonNull Movie movie) {
        Intent intent = new Intent(activity, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, IMAGE);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareWindowForAnimation();
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        ViewCompat.setTransitionName(findViewById(R.id.app_bar), IMAGE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Movie movie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        showMovie(movie);

        LoadingView loadingView = LoadingDialog.view(getSupportFragmentManager());
        mMoviesReviewSubscription = ApiFactory.getMoviesService()
                .moviesReview(movie.getId())
                .map(ReviewsResponse::getReviews)
                .flatMap(reviews -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.insert(reviews);
                    });
                    return Observable.just(reviews);
                })
                .onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Review> results = realm.where(Review.class)
                            .equalTo("id", String.valueOf(movie.getId()))
                            .findAll();
                    return Observable.just(realm.copyFromRealm(results));
                })
                .doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(loadingView::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showReviews, throwable -> showError());
    }


    @Override
    protected void onPause() {
        if (mMoviesReviewSubscription != null) {
            mMoviesReviewSubscription.unsubscribe();
        }
        super.onPause();
    }

    private void showError() {
     //
    }


    /**
         * TODO : task
         *
         * Load movie trailers and reviews and display them
         *
         * 1) See http://docs.themoviedb.apiary.io/#reference/movies/movieidtranslations/get?console=1
         * http://docs.themoviedb.apiary.io/#reference/movies/movieidtranslations/get?console=1
         * for API documentation
         *
         * 2) Add requests to {@link com.popularmovies.network.MovieService} for trailers and videos
         *
         * 3) Execute requests in parallel and show loading progress until both of them are finished
         *
         * 4) Save trailers and videos to Realm and use cached version when error occurred
         *
         * 5) Handle lifecycle changes any way you like
         */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareWindowForAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    private void showMovie(@NonNull Movie movie) {
        String title = getString(R.string.movie_details);
        mCollapsingToolbar.setTitle(title);
        mCollapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, android.R.color.transparent));

        Images.loadMovie(mImage, movie, Images.WIDTH_780);

        String year = movie.getReleasedDate().substring(0, 4);
        mTitleTextView.setText(getString(R.string.movie_title, movie.getTitle(), year));
        mOverviewTextView.setText(movie.getOverview());

        String average = String.valueOf(movie.getVoteAverage());
        average = average.length() > 3 ? average.substring(0, 3) : average;
        average = average.length() == 3 && average.charAt(2) == '0' ? average.substring(0, 1) : average;
        mRatingTextView.setText(getString(R.string.rating, average, MAXIMUM_RATING));
    }

    private void showTrailers(@NonNull List<Video> videos) {
        // TODO : show trailers
    }

    private void showReviews(@NonNull List<Review> reviews) {
        mRatingTextView.setText(reviews.get(0).getContent());
        // TODO : show reviews
    }

}
