package it.univr.francesco.flickr.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.UiThread;

import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.view.View;

public class Controller {
    private MVC mvc;

    public void setMVC(MVC mvc) {
        this.mvc = mvc;
    }

    @UiThread
    public void showList(int lastQueryID) {
        mvc.forEachView(view -> view.showList(lastQueryID));
    }

    @UiThread
    public void showPicture(int lastPictureOpened) {
        mvc.forEachView(view -> view.showPicture(lastPictureOpened));
    }

    @UiThread
    public void showAuthor() {
        mvc.forEachView(View::showAuthor);
    }

    @UiThread
    public void startService(Context context, String action, Object... objects) {
        switch (action) {
            case ExecutorIntentService.ACTION_GET_PICTURE_INFOS:
                mvc.model.clearPictureInfos();
                break;
            case ExecutorIntentService.ACTION_GET_AUTHOR_INFO:
                mvc.model.clearAuthor();
                break;
        }

        ExecutorIntentService.startService(context, action, objects);
    }

    @UiThread
    public void storePicture(int position, Bitmap picture, String type, int lastQueryID) {
        mvc.model.storePicture(position, picture, type, lastQueryID);
    }

    @UiThread
    public void storeAuthorPic(int position, Bitmap bitmap) {
        mvc.model.storeAuthorPic(position, bitmap);
    }
}