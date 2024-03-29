package it.univr.francesco.flickr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import android.widget.Toast;

import net.jcip.annotations.GuardedBy;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ImageManager {
    private final static String FLICKR_CACHE_DIR = "/FlickrCache";

    public final static String ACTION_SEND_BITMAP_PATH = "sendBitmapPath";
    final static String PARAM_BITMAP_PATH = "bitmapPath";

    private final static ConcurrentHashMap<ImageView, String> onProcessing = new ConcurrentHashMap<>();

    private final static int lruCacheSize = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
    @GuardedBy("itself") private final static LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(lruCacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    public static void display(String url, ImageView imageView) {
        onProcessing.put(imageView, url);

        Bitmap bitmap = lruCache.get(url);
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else
            new ImageDisplayer(url, imageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void share(Context context, String url, String pictureID) {
        new ImageSharer(context, url, pictureID).execute();
    }

    public static void clean() {
        new ImageCleaner().execute();
    }

    private static class ImageDisplayer extends AsyncTask<Void, Void, Bitmap> {
        private final String url;
        private final ImageView imageView;

        private ImageDisplayer(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override @UiThread
        protected void onPreExecute() {
            imageView.setImageDrawable(null);
        }

        @Override @WorkerThread
        protected Bitmap doInBackground(Void... params) {
            return downloadBitmap(url);
        }

        @Override @UiThread
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                lruCache.put(url, bitmap);

            String tmpUrl = onProcessing.get(imageView);
            if(tmpUrl != null && tmpUrl.equals(url)) {
                if (bitmap != null)
                    imageView.setImageBitmap(bitmap);
                else
                    imageView.setImageResource(R.drawable.placeholder);
            }
        }
    }

    private static class ImageSharer extends AsyncTask<Void, Void, Boolean> {
        private final Context context;
        private final String url;
        private final String pictureID;

        private ImageSharer(Context context, String url, String pictureID) {
            this.context = context;
            this.url = url;
            this.pictureID = pictureID;
        }

        @Override @WorkerThread
        protected Boolean doInBackground(Void... params) {
            Bitmap bitmap = lruCache.get(url);

            if(bitmap == null) {
                bitmap = downloadBitmap(url);
                if(bitmap == null)
                    return false;
                lruCache.put(url, bitmap);
            }

            File flickrCacheDir = new File(Environment.getExternalStorageDirectory().toString() + FLICKR_CACHE_DIR);
            if(!flickrCacheDir.exists()) flickrCacheDir.mkdirs();

            File image = new File(flickrCacheDir, String.format("%s.jpg", pictureID));

            if(!image.exists()) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(image);
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
            result.putExtra(PARAM_BITMAP_PATH, image.getAbsolutePath());
            LocalBroadcastManager.getInstance(context).sendBroadcast(result);

            return true;
        }

        @Override @UiThread
        protected void onPostExecute(Boolean success) {
            if(!success)
                Toast.makeText(context, context.getString(R.string.sharing_failed), Toast.LENGTH_LONG).show();
        }
    }

    private static class ImageCleaner extends AsyncTask<Void, Void, Void> {
        @Override @WorkerThread
        protected Void doInBackground(Void... params) {
            onProcessing.clear();

            File flickrCacheDir = new File(Environment.getExternalStorageDirectory().toString() + FLICKR_CACHE_DIR);

            if(flickrCacheDir.exists())
                for(File f: flickrCacheDir.listFiles())
                    f.delete();

            return null;
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
