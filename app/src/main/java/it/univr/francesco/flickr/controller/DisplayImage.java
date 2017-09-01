package it.univr.francesco.flickr.controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.net.URL;

import it.univr.francesco.flickr.R;

public class DisplayImage {
    private final static String TAG = DisplayImage.class.getName();

    private static LruCache<String, Bitmap> lruCache;

    private static void init() {
        if(lruCache == null) {
            final int lruCacheSize = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;

            lruCache = new LruCache<String, Bitmap>(lruCacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
    }

    public static void display(String url, ImageView imageView) {
        init();

        Bitmap bitmap = lruCache.get(url);
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else
            new ImageDownloader(url, imageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class ImageDownloader extends AsyncTask<Void, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;

        private ImageDownloader(String url, ImageView imageView) {
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
            try {
                return BitmapFactory.decodeStream(new URL(url).openStream());
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null && imageView.getTag().equals(url)) {
                lruCache.put(url, bitmap);
                imageView.setImageBitmap(bitmap);
            }
            else if(bitmap == null && imageView.getTag().equals(url)) {
                imageView.setImageResource(R.drawable.placeholder);
            }
        }
    }
}
