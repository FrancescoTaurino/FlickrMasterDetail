package it.univr.francesco.flickr.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;

public class PhoneView extends FrameLayout implements View {
    private MVC mvc;

    private FragmentManager getFragmentManager() {
        return ((Activity) getContext()).getFragmentManager();
    }

    private AbstractFragment getFragment() {
        return (AbstractFragment) getFragmentManager().findFragmentById(R.id.phone_view);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mvc = ((Flickr) getContext().getApplicationContext()).getMVC();
        mvc.register(this);

        // at the beginning, show the search fragment
        if (getFragment() == null)
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
        getFragment().onModelChanged();
    }

    @Override
    public void showList() {
        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, new ListFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showPicture() {
        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, new PictureFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAuthor() {
        getFragmentManager().beginTransaction()
                .replace(R.id.phone_view, new AuthorFragment())
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
