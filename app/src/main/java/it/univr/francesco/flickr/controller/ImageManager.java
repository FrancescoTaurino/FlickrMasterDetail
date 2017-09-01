package it.univr.francesco.flickr.controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import android.widget.Toast;

import net.jcip.annotations.GuardedBy;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import it.univr.francesco.flickr.R;

public class ImageManager {
    public final static String ACTION_SEND_BITMAP_PATH = "sendBitmapPath";
    public final static String PARAM_BITMAP_PATH = "bitmapPath";

    private final static int lruCacheSize = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
    @GuardedBy("itself") private final static LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(lruCacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private static Bitmap getFromLruCache(String url) {
        synchronized (lruCache) {
            return lruCache.get(url);
        }
    }
    private static void putInLruCache(String url, Bitmap bitmap) {
        synchronized (lruCache) {
            lruCache.put(url, bitmap);
        }
    }

    public static void display(String url, ImageView imageView) {
        Bitmap bitmap = getFromLruCache(url);
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else
            new ImageDisplayer(url, imageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    public static void share(Context context, String url) {
        new ImageSharer(context, url).execute();
    }

    private static class ImageDisplayer extends AsyncTask<Void, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;

        private ImageDisplayer(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected void onPreExecute() {
            imageView.setImageResource(R.drawable.empty);
            imageView.setTag(url);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return downloadBitmap(url);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null && imageView.getTag().equals(url)) {
                putInLruCache(url, bitmap);
                imageView.setImageBitmap(bitmap);
            }
            else if(bitmap == null && imageView.getTag().equals(url)) {
                imageView.setImageResource(R.drawable.placeholder);
            }
        }
    }

    private static class ImageSharer extends AsyncTask<Void, Void, Boolean> {
        private final Context context;
        private final String url;

        private ImageSharer(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Bitmap bitmap = getFromLruCache(url);

            if(bitmap == null) {
                bitmap = downloadBitmap(url);
                if(bitmap == null)
                    return false;
                putInLruCache(url, bitmap);
            }

            File file = new File(Environment.getExternalStorageDirectory(), String.format("%s.jpg", url.hashCode()));

            if(!file.exists()) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            Intent result = new Intent(ACTION_SEND_BITMAP_PATH);
            result.putExtra(PARAM_BITMAP_PATH, file.getAbsolutePath());
            LocalBroadcastManager.getInstance(context).sendBroadcast(result);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(!success)
                Toast.makeText(context, context.getString(R.string.sharing_failed), Toast.LENGTH_LONG).show();
        }
    }

    private static Bitmap downloadBitmap(String url) {
        try {
            return BitmapFactory.decodeStream(new URL(url).openStream());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}