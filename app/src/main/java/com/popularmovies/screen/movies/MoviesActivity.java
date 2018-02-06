package com.popularmovies.screen.movies;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import com.popularmovies.R;
import com.popularmovies.model.content.Movie;
import com.popularmovies.model.response.MoviesResponse;
import com.popularmovies.network.ApiFactory;
import com.popularmovies.network.MovieService;
import com.popularmovies.screen.details.MovieDetailsActivity;
import com.popularmovies.screen.loading.LoadingDialog;
import com.popularmovies.screen.loading.LoadingView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.realm.Realm;
import io.realm.RealmResults;

public class MoviesActivity extends AppCompatActivity implements MoviesAdapter.OnItemClickListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recyclerView)
    RecyclerView mMoviesRecycler;
    @BindView(R.id.empty)
    View mEmptyView;
    @BindView(R.id.movies_search_il)
    AutoCompleteTextView mSearchMoviesACTV;
    @Nullable
    private Disposable mMoviesDisposable;
    private LoadingView loadingView;
    private MoviesAdapter mAdapter;


    private enum MoviesType {
        Popular,
        TopRated
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        int columns = getResources().getInteger(R.integer.columns_count);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), columns);
        mMoviesRecycler.setLayoutManager(layoutManager);
        mAdapter = createAdapter();
        mMoviesRecycler.setAdapter(mAdapter);
        loadingView = LoadingDialog.view(getSupportFragmentManager());

        getMovies(MoviesType.Popular);

        RxSearchObservable.fromView(mSearchMoviesACTV)
                .debounce(500, TimeUnit.MILLISECONDS)
                .filter(text -> !text.isEmpty())
                .distinctUntilChanged()
                .switchMap(query -> ApiFactory.getMoviesService().searchMovies(query).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
                .subscribe(result -> showMovies(result.getMovies()), throwable -> showError(throwable));

    }

    @Override
    protected void onPause() {
        if (mMoviesDisposable != null) {
            mMoviesDisposable.dispose();
        }
        super.onPause();
    }

    @Override
    public void onItemClick(@NonNull View view, @NonNull Movie movie) {
        ImageView imageView = ButterKnife.findById(view, R.id.image);
        MovieDetailsActivity.navigate(this, imageView, movie);
    }

    private void showError(Throwable t) {
        mMoviesRecycler.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    private void showMovies(@NonNull List<Movie> movies) {
        mAdapter.changeDataSet(movies);
        mMoviesRecycler.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
    }

    @NonNull
    private MoviesAdapter createAdapter() {
        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.rows_count, typedValue, true);
        float rowsCount = typedValue.getFloat();
        int actionBarHeight = getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true) ? TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics()) : 0;
        int imageHeight = (int) ((getResources().getDisplayMetrics().heightPixels - actionBarHeight) / rowsCount);
        int columns = getResources().getInteger(R.integer.columns_count);
        int imageWidth = getResources().getDisplayMetrics().widthPixels / columns;

        return new MoviesAdapter(imageHeight, imageWidth, this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.movies_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_movies:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.movies_type));
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                getMovies(MoviesType.TopRated);
                break;

            case android.R.id.home:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    getSupportActionBar().setTitle(getString(R.string.app_name));

                }
                getMovies(MoviesType.Popular);
                break;
        }
        return true;
    }

    private void getMovies(MoviesType moviesType) {
        if (mMoviesDisposable != null) {
            mMoviesDisposable.dispose();
        }
        switch (moviesType) {
            case Popular:
                mMoviesDisposable = ApiFactory.getMoviesService()
                        .popularMovies()
                        .map(MoviesResponse::getMovies)
                        .flatMap(movies -> {
                            Realm.getDefaultInstance().executeTransaction(realm -> {
                                realm.delete(Movie.class);
                                realm.insert(movies);
                            });
                            return Observable.just(movies);
                        })
                        .onErrorResumeNext(throwable -> {
                            Realm realm = Realm.getDefaultInstance();
                            RealmResults<Movie> results = realm.where(Movie.class).findAll();
                            return Observable.just(realm.copyFromRealm(results));
                        })
                        .doOnSubscribe(loadingView::showLoadingIndicator)
                        .doAfterTerminate(loadingView::hideLoadingIndicator)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::showMovies, throwable -> showError(throwable));

                break;

            case TopRated:
                mMoviesDisposable = ApiFactory.getMoviesService()
                        .topRatedMovies()
                        .map(MoviesResponse::getMovies)
                        .flatMap(movies -> {
                            Realm.getDefaultInstance().executeTransaction(realm -> {
                                realm.delete(Movie.class);
                                realm.insert(movies);
                            });
                            return Observable.just(movies);
                        })
                        .onErrorResumeNext(throwable -> {
                            Realm realm = Realm.getDefaultInstance();
                            RealmResults<Movie> results = realm.where(Movie.class).findAll();
                            return Observable.just(realm.copyFromRealm(results));
                        })
                        .doOnSubscribe(loadingView::showLoadingIndicator)
                        .doAfterTerminate(loadingView::hideLoadingIndicator)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::showMovies, throwable -> showError(throwable));
                break;
        }

    }


    public static class RxSearchObservable {

        public static Observable<String> fromView(AutoCompleteTextView searchView) {

            final PublishSubject<String> subject = PublishSubject.create();

            searchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    subject.onNext(charSequence.toString());

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    //subject.onComplete();
                }
            });
            return subject;
        }
    }
}
