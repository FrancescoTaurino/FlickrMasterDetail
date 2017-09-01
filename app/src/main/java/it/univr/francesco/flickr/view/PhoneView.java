package it.univr.francesco.flickr.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;

import static it.univr.francesco.flickr.view.AuthorFragment.AUTHOR_ID;
import static it.univr.francesco.flickr.view.PictureFragment.PICTURE_ID;

public class PhoneView extends FrameLayout implements View {
    private MVC mvc;

    private FragmentManager getFragmentManager() {
        return ((Activity) getContext()).getFragmentManager();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mvc = ((Flickr) getContext().getApplicationContext()).getMVC();
        mvc.register(this);

        // at the beginning, show the search fragment
        if (getFragmentManager().findFragmentById(R.id.phone_view) == null)
            getFragmentManager().beginTransaction()
                    .add(R.id.phone_view, new SearchFragment())
                    .commit();
    }

    @Override
    protected void onDetachedFromWindow() {
        mvc.unregister(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onModelChanged() {
        ((AbstractFragment) getFragmentManager().findFragmentById(R.id.phone_view)).onModelChanged();
    }

    @Override
    public void showList() {
        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, new ListFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showPicture(String pictureID) {
        Bundle bundle = new Bundle();
        bundle.putString(PICTURE_ID, pictureID);
        PictureFragment pictureFragment = new PictureFragment();
        pictureFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, pictureFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAuthor(String authorID) {
        Bundle bundle = new Bundle();
        bundle.putString(AUTHOR_ID, authorID);
        AuthorFragment authorFragment = new AuthorFragment();
        authorFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, authorFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * These two constructors must exist to let the view be recreated at
     * configuration change or inflated from XML.
     */

    public PhoneView(Context context) {
        super(context);
    }

    public PhoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
