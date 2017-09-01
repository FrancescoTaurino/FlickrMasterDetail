package it.univr.francesco.flickr.view;

import android.support.annotation.UiThread;

public interface View {

    @UiThread
    void showList();

    @UiThread
    void showPicture(String pictureID);

    @UiThread
    void showAuthor(String authorID);

    @UiThread
    void onModelChanged();
}