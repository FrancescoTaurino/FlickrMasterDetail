package it.univr.francesco.flickr;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class Util {
    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus() != null)
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getApplicationWindowToken(), 0);
    }
}
