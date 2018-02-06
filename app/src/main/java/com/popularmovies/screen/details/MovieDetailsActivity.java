package com.popularmovies.screen.details;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

import com.popularmovies.R;
import com.popularmovies.model.content.CombinedVideoReview;
import com.popularmovies.model.content.Movie;
import com.popularmovies.model.content.Review;
import com.popularmovies.model.content.Video;
import com.popularmovies.model.response.ReviewsResponse;
import com.popularmovies.model.response.VideosResponse;
import com.popularmovies.network.ApiFactory;
import com.popularmovies.screen.loading.LoadingDialog;
import com.popularmovies.screen.loading.LoadingView;
import com.popularmovies.utils.Images;
import com.popularmovies.utils.Videos;

import org.reactivestreams.Subscription;


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

    @BindView(R.id.video_name)
    TextView mVideoKeyTextView;

    @Nullable
    private Disposable mMoviesReviewSubscription;
    @Nullable
    private Video mVideo;
    @Nullable
    private Movie mMovie;

    public static void navigate(@NonNull AppCompatActivity activity, @NonNull View transitionImage,
                                @NonNull Movie movie) {
        Intent intent = new Intent(activity, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, IMAGE);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }


    private Observable<ReviewsResponse> getReviewsObservable(int id) {
        return ApiFactory.getMoviesService().movieReviews(id);
    }

    private Observable<VideosResponse> getVideosObservable(int id) {
        return ApiFactory.getMoviesService().movieTrailers(id);
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
        mVideoKeyTextView.setPaintFlags(mVideoKeyTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mMovie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        showMovie(mMovie);
        LoadingView loadingView = LoadingDialog.view(getSupportFragmentManager());
        Observable.zip(
                getReviewsObservable(mMovie.getId()).subscribeOn(Schedulers.newThread()),
                getVideosObservable(mMovie.getId()).subscribeOn(Schedulers.newThread()),
                (reviewsResponse, videosResponse) -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.insertOrUpdate(videosResponse.getVideos());
                        realm.insertOrUpdate(reviewsResponse.getReviews());
                    });
                    return new CombinedVideoReview(videosResponse.getVideos(), reviewsResponse.getReviews());
                })
                .onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Review> reviewRealmResults = realm.where(Review.class)
                            .equalTo("mId", mMovie.getId())
                            .findAll();
                    RealmResults<Video> videoRealmResults = realm.where(Video.class)
                            .equalTo("mId", mMovie.getId())
                            .findAll();
                    return Observable.just(new CombinedVideoReview(realm.copyFromRealm(videoRealmResults), realm.copyFromRealm(reviewRealmResults)));
                })
                .doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(loadingView::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showCombinedVideoTrailers, this::showError);

    }


    @Override
    protected void onPause() {
        if (mMoviesReviewSubscription != null) {
            mMoviesReviewSubscription.dispose();
        }
        super.onPause();
    }

    private void showError(Throwable throwable) {
        Snackbar.make(mRatingTextView, throwable.getMessage(), Snackbar.LENGTH_SHORT);
    }

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

    private void showCombinedVideoTrailers(@NonNull CombinedVideoReview combinedVideoReview) {
        mVideo = combinedVideoReview.getVideoList().get(0);
        mVideoKeyTextView.setText(mVideo.getName());
        mReviewTextView.setText(combinedVideoReview.getReviewList().get(0).getContent());
    }

    @OnClick(R.id.video_name)
    public void onVideoPathClick() {
        assert mVideo != null;
        Videos.browseVideo(this, mVideo);
    }
}
