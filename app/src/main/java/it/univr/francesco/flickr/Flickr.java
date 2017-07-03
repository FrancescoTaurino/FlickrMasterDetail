package it.univr.francesco.flickr;

import android.app.Application;

import it.univr.francesco.flickr.controller.Controller;
import it.univr.francesco.flickr.model.Model;

public class Flickr extends Application {
    private MVC mvc;

    @Override
    public void onCreate() {
        super.onCreate();

        mvc = new MVC(new Model(), new Controller());
    }

    public MVC getMVC() {
        return mvc;
    }
}