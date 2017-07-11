package it.univr.francesco.flickr.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;

public class TabletView extends LinearLayout implements View {
    private MVC mvc;

    private FragmentManager getFragmentManager() {
        return ((Activity) getContext()).getFragmentManager();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mvc = ((Flickr) getContext().getApplicationContext()).getMVC();
        mvc.register(this);

        if (getFragmentManager().findFragmentById(R.id.tablet_view) == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.detail_fragment, new ListFragment())
                    .commit();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mvc.unregister(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onModelChanged() {
        ((AbstractFragment) getFragmentManager().findFragmentById(R.id.master_fragment)).onModelChanged();
        ((AbstractFragment) getFragmentManager().findFragmentById(R.id.detail_fragment)).onModelChanged();
    }

    @Override
    public void showList() {
        getFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, new ListFragment())
                .commit();
    }

    @Override
    public void showPicture() {
        getFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, new PictureFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAuthor() {
        getFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, new AuthorFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * These two constructors must exist to let the view be recreated at
     * configuration change or inflated from XML.
     */

    public TabletView(Context context) {
        super(context);
    }

    public TabletView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
