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

    private final static String APY_KEY = "a047cb41da0f59c9659c44819e951b58";

    private final static String[] QUERIES = {
            "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=%s&text=%s&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",     // 0 -> Search by string
            "https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=%s&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",          // 1 -> Search recent
            "https://api.flickr.com/services/rest/?method=flickr.interestingness.getList&api_key=%s&extras=owner_name&per_page=" + PICTURES_LIST_NUMBER + "&format=rest",   // 2 -> Search popular
            "https://api.flickr.com/services/rest/?method=flickr.photos.comments.getList&api_key=%s&photo_id=%s&format=rest",                                               // 3 -> Get Comments
            "https://api.flickr.com/services/rest/?method=flickr.people.getInfo&api_key=%s&user_id=%s&format=rest",                                                         // 4 -> Get General author infos
            "https://api.flickr.com/services/rest/?method=flickr.people.getPublicPhotos&api_key=%s&user_id=%s&per_page=" + PICTURES_AUTHOR_NUMBER + "&format=rest"          // 5 -> Get recent upload urls of author
    };

    private final static String IMAGE_BASE_URL = "https://farm%s.staticflickr.com/%s/%s_%s_%s.jpg";
    private final static String PROFILE_IMAGE_BASE_URL = "http://farm%s.staticflickr.com/%s/buddyicons/%s_l.jpg";

    public final static String ACTION_GET_PICTURE_INFOS = "getPictureInfos";
    public final static String ACTION_GET_PREVIEW = "getPreview";
    public final static String ACTION_GET_PICTURE = "getPicture";
    public final static String ACTION_GET_COMMENTS = "getComments";
    public final static String ACTION_GET_AUTHOR_INFO_GENERAL = "getAuthorInfos";
    public final static String ACTION_GET_RECENT_UPLOADS_URLS = "getRecentUploadsURLs";
    public final static String ACTION_GET_RECENT_UPLOADS_PIC = "getRecentUploadsPic";
    public final static String ACTION_SHARE_PICTURE = "sharePicture";
    public final static String ACTION_SEND_BITMAP_PATH = "sendBitmapPath";
    public final static String ACTION_CLEAR_PICTURE_FOLDER = "clearPictureFolder";

    private final static String PARAM_STRING_TO_SEARCH = "stringToSearch";
    private final static String PARAM_WHICH_QUERY = "whichQuery";
    private final static String PARAM_POSITION = "position";
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
                break;
            case ACTION_GET_PREVIEW:
            case ACTION_GET_PICTURE:
            case ACTION_GET_COMMENTS:
            case ACTION_GET_AUTHOR_INFO_GENERAL:
            case ACTION_GET_RECENT_UPLOADS_URLS:
            case ACTION_GET_RECENT_UPLOADS_PIC:
            case ACTION_SHARE_PICTURE:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
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
        executorService.execute(() -> {
            onHandleIntent(intent);
            EDT.post(this::endOfTask);
        });

        return START_NOT_STICKY;
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
        int position;
        String authorID;
        Model.PictureInfo pictureInfo;

        switch (intent.getAction()) {
            case ACTION_GET_PICTURE_INFOS:
                String stringToSearch = (String) intent.getSerializableExtra(PARAM_STRING_TO_SEARCH);
                int whichQuery = (int) intent.getSerializableExtra(PARAM_WHICH_QUERY);
                Model.PictureInfo[] pictureInfos;

                try {
                    pictureInfos = getPictureInfos(stringToSearch, whichQuery);
                }
                catch (Exception e) {
                    pictureInfos = new Model.PictureInfo[PICTURES_LIST_NUMBER];
                }

                mvc.model.storePictureInfos(pictureInfos);
                break;
            case ACTION_GET_PREVIEW:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                if ((pictureInfo = mvc.model.getPictureInfoAtPosition(position)) == null) break;
                String previewURL = pictureInfo.previewURL;

                mvc.model.addPictureToCache(position, getBitmap(previewURL), PICTURE_SMALL);
                break;
            case ACTION_GET_PICTURE:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                if ((pictureInfo = mvc.model.getPictureInfoAtPosition(position)) == null) break;
                String pictureURL = pictureInfo.pictureURL;

                mvc.model.addPictureToCache(position, getBitmap(pictureURL), PICTURE_LARGE);
                break;
            case ACTION_GET_COMMENTS:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                if ((pictureInfo = mvc.model.getPictureInfoAtPosition(position)) == null) break;
                String pictureID = pictureInfo.pictureID;
                List<Model.PictureInfo.Comment> comments;

                try {
                    comments = getComments(pictureID);
                }
                catch (Exception e) {
                    comments = Collections.emptyList();
                }

                mvc.model.storeCommentsOfPictureInfoAtPosition(position, comments);
                break;
            case ACTION_GET_AUTHOR_INFO_GENERAL:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                if ((pictureInfo = mvc.model.getPictureInfoAtPosition(position)) == null) break;
                authorID = pictureInfo.authorID;
                Model.AuthorInfo.AuthorInfoGeneral authorInfoGeneral;

                try {
                    authorInfoGeneral = getAuthorInfoGeneral(authorID);
                }
                catch (Exception e) {
                   authorInfoGeneral = null;
                }

                mvc.model.storeAuthorInfoGeneral(authorInfoGeneral);
                break;
            case ACTION_GET_RECENT_UPLOADS_URLS:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                if ((pictureInfo = mvc.model.getPictureInfoAtPosition(position)) == null) break;
                authorID = pictureInfo.authorID;
                String[] recentUploadsURLs;

                try {
                    recentUploadsURLs = getRecentUploadsURLs(authorID);
                }
                catch (Exception e) {
                    recentUploadsURLs = new String[PICTURES_AUTHOR_NUMBER];
                }

                mvc.model.addURLsToRecentUploads(recentUploadsURLs);
                break;
            case ACTION_GET_RECENT_UPLOADS_PIC:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                String recentUploadURL = (String) mvc.model.getFromRecentUploadsAtPosition(position, Model.URL);

                mvc.model.addPicToRecentUploads(position, getBitmap(recentUploadURL));
                break;
            case ACTION_SHARE_PICTURE:
                position = (int) intent.getSerializableExtra(PARAM_POSITION);
                Bitmap picture;

                if(mvc.model.getPictureFromCache(position, Model.PICTURE_LARGE) == null) {
                    picture = getBitmap(mvc.model.getPictureInfoAtPosition(position).pictureURL);
                    mvc.model.addPictureToCache(position, picture, Model.PICTURE_LARGE);
                }
                else
                    picture = mvc.model.getPictureFromCache(position, PICTURE_LARGE);

                File dir = new File(Environment.getExternalStorageDirectory().toString() + PICTURE_FOLDER);
                if(!dir.exists()) dir.mkdirs();

                File file = new File(dir, String.format("%s.jpg", mvc.model.getPictureInfoAtPosition(position).pictureID));

                if(!file.exists()) {
                    try {
                        storeBitmap(file, picture);
                    }
                    catch (Exception e) {
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
    private Model.PictureInfo[] getPictureInfos(String stringToSearch, int whichQuery) throws Exception {
        String query = whichQuery == 0 ?
                String.format(QUERIES[whichQuery], APY_KEY, URLEncoder.encode(stringToSearch, "UTF-8")) :
                String.format(QUERIES[whichQuery], APY_KEY);

        Log.d(TAG, query);

        return parsePictureInfosXML(getXMLFromQuery(query));
    }

    @WorkerThread
    private List<Model.PictureInfo.Comment> getComments(String pictureID) throws Exception {
        String query = String.format(QUERIES[3], APY_KEY, pictureID);

        Log.d(TAG, query);

        return parseCommentsXML(getXMLFromQuery(query));
    }

    @WorkerThread
    private Model.AuthorInfo.AuthorInfoGeneral getAuthorInfoGeneral(String authorID) throws Exception {
        String query = String.format(QUERIES[4], APY_KEY, authorID);

        Log.d(TAG, query);

        return parseAuthorInfoGeneralXML(getXMLFromQuery(query));
    }

    private String[] getRecentUploadsURLs(String author_id) throws Exception {
        String query = String.format(QUERIES[5], APY_KEY, author_id);

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
    private Model.PictureInfo[] parsePictureInfosXML(String xml) throws Exception {
        Model.PictureInfo[] pictureInfos = new Model.PictureInfo[PICTURES_LIST_NUMBER];

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String id, title, author_id, author_name, farm, server, secret;
        nodeList = document.getElementsByTagName("photo");

        for (int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            id = element.getAttribute("id");
            title = element.getAttribute("title");
            author_id = element.getAttribute("owner");
            author_name = element.getAttribute("ownername");

            farm = element.getAttribute("farm");
            server = element.getAttribute("server");
            secret = element.getAttribute("secret");

            String previewURL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "s");
            String pictureURL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "h");

            pictureInfos[i] = new Model.PictureInfo(id, title, author_id, author_name, previewURL, pictureURL);
        }

        return pictureInfos;
    }

    @WorkerThread
    private List<Model.PictureInfo.Comment> parseCommentsXML(String xml) throws Exception {
        LinkedList<Model.PictureInfo.Comment> comments = new LinkedList<>();

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String author, text;
        nodeList = document.getElementsByTagName("comment");

        for(int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            author = element.getAttribute("authorname");
            text = element.getTextContent();

            comments.add(new Model.PictureInfo.Comment(author, text));
        }

        return comments;
    }

    @WorkerThread
    private Model.AuthorInfo.AuthorInfoGeneral parseAuthorInfoGeneralXML(String xml) throws Exception {
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

        return new Model.AuthorInfo.AuthorInfoGeneral(profile_image, username, realname, location, description);
    }

    @WorkerThread
    private String[] parseRecentUploadsURLsXML(String xml) throws Exception {
        String[] recentUploadsURLs = new String[PICTURES_AUTHOR_NUMBER];

        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String id, farm, server, secret;
        nodeList = document.getElementsByTagName("photo");

        for (int i = 0; i < nodeList.getLength(); i++) {
            element = (Element) nodeList.item(i);

            id = element.getAttribute("id");
            farm = element.getAttribute("farm");
            server = element.getAttribute("server");
            secret = element.getAttribute("secret");

            String URL = String.format(IMAGE_BASE_URL, farm, server, id, secret, "q");

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