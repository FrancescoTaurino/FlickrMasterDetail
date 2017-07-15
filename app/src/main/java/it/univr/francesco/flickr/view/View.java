package it.univr.francesco.flickr.view;

import android.support.annotation.UiThread;

public interface View {

    @UiThread
    void showList(int lastQueryID);

    @UiThread
    void showPicture(int lastPictureOpened);

    @UiThread
    void showAuthor();

    @UiThread
    void onModelChanged();
}