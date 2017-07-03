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
    public void showList() {
        mvc.forEachView(View::showList);
    }

    @UiThread
    public void showPicture() {
        mvc.forEachView(View::showPicture);
    }

    @UiThread
    public void showAuthor() {
        mvc.forEachView(View::showAuthor);
    }

    @UiThread
    public void startService(Context context, String action, Object... objects) {
        switch (action) {
            case ExecutorIntentService.ACTION_GET_PICTURE_INFOS:
                mvc.model.clearModel();
                break;
            case ExecutorIntentService.ACTION_GET_AUTHOR_INFO_GENERAL:
                mvc.model.clearAuthorInfo();
                break;
        }

        ExecutorIntentService.startService(context, action, objects);
    }

    @UiThread
    public void addPictureToCache(int position, Bitmap picture, String type) {
        mvc.model.addPictureToCache(position, picture, type);
    }

    @UiThread
    public void addPicToRecentUploads(int position, Bitmap pic) {
        mvc.model.addPicToRecentUploads(position, pic);
    }

    @UiThread
    public void setLastPictureOpened(int position) {
        mvc.model.setLastPictureOpened(position);
    }
}