package it.univr.francesco.flickr.view;

import android.support.annotation.UiThread;

public interface AbstractFragment {

    @UiThread
    void onModelChanged();
}