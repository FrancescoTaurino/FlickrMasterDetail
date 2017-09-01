package it.univr.francesco.flickr.controller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.model.Model;

import static it.univr.francesco.flickr.model.Model.PICTURES_AUTHOR_NUMBER;
import static it.univr.francesco.flickr.model.Model.PICTURES_LIST_NUMBER;
import static it.univr.francesco.flickr.model.Model.PICTURE_LARGE;
import static it.univr.francesco.flickr.model.Model.PICTURE_SMALL;

public class ExecutorIntentService extends Service {
    private final static String TAG = ExecutorIntentService.class.getName();
    private final static int nCores = Runtime.getRuntime().availableProcessors();
    private final Handler EDT = new Handler(Looper.getMainLooper());

    private final static String API_KEY = "a047cb41da0f59c9659c44819e951b58";
    private final static String QUERY_BASE = "https://api.flickr.com/services/rest/?method=flickr";

    private final static String[] QUERIES = {
            QUERY_BASE + ".photos.search&api_key=" + API_KEY + "&text=%s&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",     // 0 -> Search by string
            QUERY_BASE + ".photos.getRecent&api_key=" + API_KEY + "&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",          // 1 -> Search recent
            QUERY_BASE + ".interestingness.getList&api_key=" + API_KEY + "&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",   // 2 -> Search popular
            QUERY_BASE + ".photos.comments.getList&api_key=" + API_KEY + "&photo_id=%s&format=rest",                                               // 3 -> Get Comments
            QUERY_BASE + ".people.getInfo&api_key=" + API_KEY + "&user_id=%s&format=rest",                                                         // 4 -> Get General author infos
            QUERY_BASE + ".people.getPublicPhotos&api_key=" + API_KEY + "&user_id=%s&per_page=" + PICTURES_AUTHOR_NUMBER + "&format=rest"          // 5 -> Get recent upload urls of author
    };

    private final static String IMAGE_BASE_URL = "https://farm%s.staticflickr.com/%s/%s_%s_%s.jpg";
    private final static String PROFILE_IMAGE_BASE_URL = "http://farm%s.staticflickr.com/%s/buddyicons/%s_l.jpg";

    public final static String ACTION_GET_PICTURE_INFOS = "getPictureInfos";
    public final static String ACTION_GET_PREVIEW = "getPreview";
    public final static String ACTION_GET_PICTURE = "getPicture";
    public final static String ACTION_GET_COMMENTS = "getComments";
    public final static String ACTION_GET_AUTHOR_INFO = "getAuthorInfos";
    public final static String ACTION_GET_RECENT_UPLOADS_URLS = "getRecentUploadsURLs";
    public final static String ACTION_GET_RECENT_UPLOADS_PIC = "getRecentUploadsPic";
    public final static String ACTION_SHARE_PICTURE = "sharePicture";
    public final static String ACTION_SEND_BITMAP_PATH = "sendBitmapPath";
    public final static String ACTION_CLEAR_PICTURE_FOLDER = "clearPictureFolder";

    private final static String PARAM_STRING_TO_SEARCH = "stringToSearch";
    private final static String PARAM_WHICH_QUERY = "whichQuery";
    private final static String PARAM_LAST_QUERY_ID = "lastQueryID";
    private final static String PARAM_POSITION = "position";
    private final static String PARAM_PREVIEW_URL = "previewURL";
    private final static String PARAM_PICTURE_URL = "pictureURL";
    private final static String PARAM_PICTURE_ID = "pictureID";
    private final static String PARAM_AUTHOR_ID = "authorID";
    private final static String PARAM_RECENT_UPLOADS_URL = "recentUploadsURL";
    private final static String PARAM_IS_DOWNLOADED = "isDownloaded";
    public final static String PARAM_BITMAP_PATH = "bitmapPath";

    private final static String PICTURE_FOLDER = "/MyFlickr";

    private MVC mvc;
    private ExecutorService executorService;
    private int runningTasks;

    @UiThread
    public static void startService(Context context, String action, Object... objects) {
        Intent intent = new Intent(context, ExecutorIntentService.class);

        switch (action) {
            case ACTION_GET_PICTURE_INFOS:
                intent.setAction(action);
                intent.putExtra(PARAM_STRING_TO_SEARCH, (String) objects[0]);
                intent.putExtra(PARAM_WHICH_QUERY, (int) objects[1]);
                intent.putExtra(PARAM_LAST_QUERY_ID, (int) objects[2]);
                break;
            case ACTION_GET_PREVIEW:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_PREVIEW_URL, (String) objects[1]);
                intent.putExtra(PARAM_LAST_QUERY_ID, (int) objects[2]);
                break;
            case ACTION_GET_PICTURE:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_PICTURE_URL, (String) objects[1]);
                break;
            case ACTION_GET_COMMENTS:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_PICTURE_ID, (String) objects[1]);
                break;
            case ACTION_GET_AUTHOR_INFO:
            case ACTION_GET_RECENT_UPLOADS_URLS:
                intent.setAction(action);
                intent.putExtra(PARAM_AUTHOR_ID, (String) objects[0]);
                break;
            case ACTION_GET_RECENT_UPLOADS_PIC:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_RECENT_UPLOADS_URL, (String) objects[1]);
                break;
            case ACTION_SHARE_PICTURE:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_IS_DOWNLOADED, (boolean) objects[1]);
                break;
            case ACTION_CLEAR_PICTURE_FOLDER:
                intent.setAction(action);
                break;
        }

        context.startService(intent);
    }

    @Override @UiThread
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(nCores);
        mvc = ((Flickr) getApplication()).getMVC();
    }

    @UiThread
    private void endOfTask() {
        if (--runningTasks == 0)
            stopSelf();
    }

    @Override @UiThread
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        runningTasks++;

        executorService.execute(() -> {
            onHandleIntent(intent);
            EDT.post(this::endOfTask);
        });

        return START_REDELIVER_INTENT;
    }

    @Override @UiThread
    public void onDestroy() {
        executorService.shutdown();
    }

    @Override @UiThread
    public IBinder onBind(Intent intent) {
        return null;
    }

    @WorkerThread
    protected void onHandleIntent(Intent intent) {
        int position, whichQuery, lastQueryID;
        String stringToSearch, previewURL, pictureURL, pictureID, authorID, recentUploadsURL;
        Boolean isDownloaded;

        switch (intent.getAction()) {
            case ACTION_GET_PICTURE_INFOS:
                stringToSearch = (String) intent.getSerializableExtra(PARAM_STRING_TO_SEARCH);
                whichQuery = (int) intent.getSerializableExtra(PARAM_WHICH_QUERY);
                lastQueryID = (int) intent.getSerializableExtra(PARAM_LAST_QUERY_ID);
                List<Model.PictureInfo> pictureInfos;

                try {
                    pictureInfos = getPictureInfos(stringToSearch, whichQuery);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    pictureInfos = Collections.emptyList();
                }

                mvc.model.storePictureInfos(pictureInfos, lastQueryID);
                break;
            case ACTION_GET_PREVIEW:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                previewURL = (String) intent.getSerializableExtra(PARAM_PREVIEW_URL);
                lastQueryID = (int) intent.getSerializableExtra(PARAM_LAST_QUERY_ID);

                mvc.model.storePicture(position, getBitmap(previewURL), PICTURE_SMALL, lastQueryID);
                break;
            case ACTION_GET_PICTURE:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                pictureURL = (String) intent.getSerializableExtra(PARAM_PICTURE_URL);

                mvc.model.storePicture(position, getBitmap(pictureURL), PICTURE_LARGE, -1);
                break;
            case ACTION_GET_COMMENTS:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                pictureID = (String) intent.getSerializableExtra(PARAM_PICTURE_ID);
                List<String> comments;

                try {
                    comments = getComments(pictureID);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    comments = Collections.emptyList();
                }

                mvc.model.storeComments(position, comments);
                break;
            case ACTION_GET_AUTHOR_INFO:
                authorID = (String) intent.getSerializableExtra(PARAM_AUTHOR_ID);
                Model.Author.AuthorInfo authorInfo;

                try {
                    authorInfo = getAuthorInfo(authorID);
                }
                catch (Exception e) {
                    e.printStackTrace();
                   authorInfo = null;
                }

                mvc.model.storeAuthorInfo(authorInfo);
                break;
            case ACTION_GET_RECENT_UPLOADS_URLS:
                authorID = (String) intent.getSerializableExtra(PARAM_AUTHOR_ID);
                String[] recentUploadsURLs;

                try {
                    recentUploadsURLs = getRecentUploadsURLs(authorID);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    recentUploadsURLs = new String[PICTURES_AUTHOR_NUMBER];
                }

                mvc.model.storeAuthorURLs(recentUploadsURLs);
                break;
            case ACTION_GET_RECENT_UPLOADS_PIC:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                recentUploadsURL = (String) intent.getSerializableExtra(PARAM_RECENT_UPLOADS_URL);

                mvc.model.storeAuthorPic(position, getBitmap(recentUploadsURL));
                break;
            case ACTION_SHARE_PICTURE:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                isDownloaded = (boolean) intent.getSerializableExtra(PARAM_IS_DOWNLOADED);
                Bitmap picture;

                if(!isDownloaded) {
                    picture = getBitmap(mvc.model.getPictureInfo(position).pictureURL);
                    mvc.model.storePicture(position, picture, Model.PICTURE_LARGE, -1);
                }
                else
                    picture = mvc.model.getPictureInfo(position).getPicture(PICTURE_LARGE);

                File dir = new File(Environment.getExternalStorageDirectory().toString() + PICTURE_FOLDER);
                if(!dir.exists()) dir.mkdirs();

                File file = new File(dir, String.format("%s.jpg", mvc.model.getPictureInfo(position).pictureID));

                if(!file.exists()) {
                    try {
                        storeBitmap(file, picture);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        EDT.post(() -> Toast.makeText(getApplicationContext(), getResources().getString(R.string.sharing_failed), Toast.LENGTH_LONG).show());
                        break;
                    }
                }

                sendIntent(file);
                break;
            case ACTION_CLEAR_PICTURE_FOLDER:
                File pictureFolder = new File(Environment.getExternalStorageDirectory().toString() + PICTURE_FOLDER);

                if(pictureFolder.exists())
                    for(File f: pictureFolder.listFiles())
                        f.delete();
                break;
        }
    }

    @WorkerThread
    private List<Model.PictureInfo> getPictureInfos(String stringToSearch, int whichQuery) throws Exception {
        String query = whichQuery == 0 ?
                String.format(QUERIES[whichQuery], URLEncoder.encode(stringToSearch, "UTF-8")) : QUERIES[whichQuery];

        Log.d(TAG, query);

        return parsePictureInfosXML(getXMLFromQuery(query));
    }

    @WorkerThread
    private List<String> getComments(String pictureID) throws Exception {
        String query = String.format(QUERIES[3], pictureID);

        Log.d(TAG, query);

        return parseCommentsXML(getXMLFromQuery(query));
    }

    @WorkerThread
    private Model.Author.AuthorInfo getAuthorInfo(String authorID) throws Exception {
        String query = String.format(QUERIES[4], authorID);

        Log.d(TAG, query);

        return parseAuthorInfoXML(getXMLFromQuery(query));
    }

    private String[] getRecentUploadsURLs(String author_id) throws Exception {
        String query = String.format(QUERIES[5], author_id);

        Log.d(TAG, query);

        return parseRecentUploadsURLsXML(getXMLFromQuery(query));
    }

    @WorkerThread
    private String getXMLFromQuery(String query) throws Exception {
        URLConnection conn = new URL(query).openConnection();
        String answerXML = "";

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                answerXML += line + "\n";
                //Log.d(TAG, line);
            }
        }
        finally {
            if (in != null)
                in.close();
        }

        return answerXML;
    }

    @WorkerThread
    private List<Model.PictureInfo> parsePictureInfosXML(String xml) throws Exception {
        List<Model.PictureInfo> pictureInfos = new LinkedList<>();

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String id, title, author_id, author_name, farm, server, secret, caption, previewURL, pictureURL;
        nodeList = document.getElementsByTagName("photo");

        for (int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            id = element.getAttribute("id");
            title = element.getAttribute("title");
            author_id = element.getAttribute("owner");
            author_name = element.getAttribute("ownername");

            caption = String.format("<b>%s</b><br><br><font color=\"gray\">%s</font>", title, author_name);

            farm = element.getAttribute("farm");
            server = element.getAttribute("server");
            secret = element.getAttribute("secret");

            previewURL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "s");
            pictureURL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "h");

            pictureInfos.add(new Model.PictureInfo(id, author_id, caption, previewURL, pictureURL));
        }

        return pictureInfos;
    }

    @WorkerThread
    private List<String> parseCommentsXML(String xml) throws Exception {
        List<String> comments = new LinkedList<>();

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String author, text;
        nodeList = document.getElementsByTagName("comment");

        for(int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            author = element.getAttribute("authorname");
            text = element.getTextContent();

            comments.add(String.format("<b><font color='black'>%s: </font></b>%s", author, text));
        }

        return comments;
    }

    @WorkerThread
    private Model.Author.AuthorInfo parseAuthorInfoXML(String xml) throws Exception {
        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String nsid, iconserver, iconfarm, username, realname, location, description;
        Bitmap profile_image;

        element = (Element) document.getElementsByTagName("person").item(0);
        nsid = element.getAttribute("nsid");
        iconserver = element.getAttribute("iconserver");
        iconfarm = element.getAttribute("iconfarm");
        if(Integer.parseInt(iconserver) > 0)
            profile_image = getBitmap(String.format(PROFILE_IMAGE_BASE_URL, iconfarm, iconserver, nsid));
        else
            profile_image = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder);


        nodeList = document.getElementsByTagName("username");
        username = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("realname");
        realname = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("location");
        location = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("description");
        description = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        return new Model.Author.AuthorInfo(profile_image, username, realname, location, description);
    }

    @WorkerThread
    private String[] parseRecentUploadsURLsXML(String xml) throws Exception {
        String[] recentUploadsURLs = new String[PICTURES_AUTHOR_NUMBER];

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String id, farm, server, secret, URL;
        nodeList = document.getElementsByTagName("photo");

        for (int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            id = element.getAttribute("id");
            farm = element.getAttribute("farm");
            server = element.getAttribute("server");
            secret = element.getAttribute("secret");

            URL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "q");

            recentUploadsURLs[i] = URL;
        }

        return recentUploadsURLs;
    }

    @WorkerThread
    private Document buildDocumentXML(String xml) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = document.getDocumentElement();
        root.normalize();

        return document;
    }

    @WorkerThread
    private Bitmap getBitmap(String URL) {
        try {
            return BitmapFactory.decodeStream(new URL(URL).openStream());
        }
        catch (Exception e) {
            //e.printStackTrace();
            return BitmapFactory.decodeResource(getResources(), R.drawable.placeholder);
        }
    }

    @WorkerThread
    private void storeBitmap(File file, Bitmap picture) throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        picture.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    @WorkerThread
    private void sendIntent(File file) {
        Intent result = new Intent(ACTION_SEND_BITMAP_PATH);
        result.putExtra(PARAM_BITMAP_PATH, file.getAbsolutePath());
        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
    }
}