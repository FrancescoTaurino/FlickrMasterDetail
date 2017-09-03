package it.univr.francesco.flickr.controller;

import android.content.Context;
import android.support.annotation.UiThread;

import it.univr.francesco.flickr.ImageManager;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.view.View;

import static it.univr.francesco.flickr.controller.ExecutorService.ACTION_GET_PICTURE_INFOS;

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
    public void showPicture(String pictureID) {
        mvc.forEachView(view -> view.showPicture(pictureID));
    }

    @UiThread
    public void showAuthor(String authorID) {
        mvc.forEachView(view -> view.showAuthor(authorID));
    }

    @UiThread
    public void startService(Context context, String action, Object... objects) {
        if(action.equals(ACTION_GET_PICTURE_INFOS)) {
            ImageManager.clean();
            mvc.model.clearModel();
        }

        ExecutorService.startService(context, action, objects);
    }
}