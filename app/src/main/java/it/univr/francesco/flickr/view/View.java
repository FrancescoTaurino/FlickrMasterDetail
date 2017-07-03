package it.univr.francesco.flickr.view;

import android.support.annotation.UiThread;

public interface View {

    @UiThread
    void showList();

    @UiThread
    void showPicture();

    @UiThread
    void showAuthor();

    @UiThread
    void onModelChanged();
}