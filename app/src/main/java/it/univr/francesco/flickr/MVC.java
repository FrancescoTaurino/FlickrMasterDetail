package it.univr.francesco.flickr;

import android.os.Handler;
import android.os.Looper;

import net.jcip.annotations.ThreadSafe;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import it.univr.francesco.flickr.controller.Controller;
import it.univr.francesco.flickr.model.Model;
import it.univr.francesco.flickr.view.View;

@ThreadSafe
public class MVC {
    public final Model model;
    public final Controller controller;
    private final List<View> views = new CopyOnWriteArrayList<>();

    public MVC(Model model, Controller controller) {
        this.model = model;
        this.controller = controller;

        model.setMVC(this);
        controller.setMVC(this);
    }

    public void register(View view) {
        views.add(view);
    }

    public void unregister(View view) {
        views.remove(view);
    }

    public interface ViewTask {
        void process(View view);
    }

    public void forEachView(ViewTask task) {
        // run a Runnable in the UI thread
        new Handler(Looper.getMainLooper()).post(() -> {
            for (View view: views)
                task.process(view);
        });
    }
}