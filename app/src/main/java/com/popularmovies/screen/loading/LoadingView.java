package com.popularmovies.screen.loading;

/**
 * @author Artur Vasilov
 */
public interface LoadingView {

    void hideLoadingIndicator();

    void showLoadingIndicator(Object disposable);
}
