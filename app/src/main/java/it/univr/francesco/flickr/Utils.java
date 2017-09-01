package it.univr.francesco.flickr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.inputmethod.InputMethodManager;

import java.io.File;

import static it.univr.francesco.flickr.controller.ImageManager.PARAM_BITMAP_PATH;

public class Utils {
    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus() != null)
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getApplicationWindowToken(), 0);
    }

    public static Intent getIntentToShare(Intent incomingIntent) {
        String bitmapPath = (String) incomingIntent.getSerializableExtra(PARAM_BITMAP_PATH);

        return new Intent(Intent.ACTION_SEND)
                .setType("image/jpg")
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(bitmapPath)));

    }
}
