package it.univr.francesco.flickr.controller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import it.univr.francesco.flickr.model.Model;

public class ExecutorIntentService extends Service {
    private final static String TAG = ExecutorIntentService.class.getName();
    private final static int nCores = Runtime.getRuntime().availableProcessors();
    private final Handler EDT = new Handler(Looper.getMainLooper());

    private final static int PICTURES_LIST_NUMBER = 30;
    private final static int PICTURES_AUTHOR_NUMBER = 15;

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
    private final static String PROFILE_IMAGE_DEFAULT_URL = "https://www.flickr.com/images/buddyicon.gif";

    public final static String ACTION_GET_PICTURE_INFOS = "getPictureInfos";
    public final static String ACTION_GET_COMMENTS = "getComments";
    public final static String ACTION_GET_AUTHOR_INFOS = "getAuthorInfos";
    public final static String ACTION_SHARE_PICTURE = "sharePicture";

    private final static String PARAM_STRING_TO_SEARCH = "stringToSearch";
    private final static String PARAM_WHICH_QUERY = "whichQuery";
    private final static String PARAM_POSITION = "position";
    private final static String PARAM_PICTURE_ID = "pictureID";
    private final static String PARAM_AUTHOR_ID = "authorID";
    private final static String PARAM_IS_DOWNLOADED = "isDownloaded";


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
            case ACTION_GET_COMMENTS:
                intent.setAction(action);
                intent.putExtra(PARAM_PICTURE_ID, (String) objects[0]);
                break;
            case ACTION_GET_AUTHOR_INFOS:
                intent.setAction(action);
                intent.putExtra(PARAM_AUTHOR_ID, (String) objects[0]);
                break;
            case ACTION_SHARE_PICTURE:
                intent.setAction(action);
                intent.putExtra(PARAM_POSITION, (int) objects[0]);
                intent.putExtra(PARAM_IS_DOWNLOADED, (boolean) objects[1]);
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
        int whichQuery;
        String stringToSearch, pictureID, authorID;

        switch (intent.getAction()) {
            case ACTION_GET_PICTURE_INFOS:
                stringToSearch = (String) intent.getSerializableExtra(PARAM_STRING_TO_SEARCH);
                whichQuery = (int) intent.getSerializableExtra(PARAM_WHICH_QUERY);
                List<Model.PictureInfo> pictureInfos;

                try {
                    pictureInfos = getPictureInfos(stringToSearch, whichQuery);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    pictureInfos = Collections.emptyList();
                }

                mvc.model.storePictureInfos(pictureInfos);
                break;
            case ACTION_GET_COMMENTS:
                pictureID = (String) intent.getSerializableExtra(PARAM_PICTURE_ID);
                List<String> comments;

                try {
                    comments = getComments(pictureID);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    comments = Collections.emptyList();
                }

                mvc.model.storeComments(pictureID, comments);
                break;
            case ACTION_GET_AUTHOR_INFOS:
                authorID = (String) intent.getSerializableExtra(PARAM_AUTHOR_ID);
                Model.AuthorInfo authorInfo;
                List<String> urls;

                try {
                    authorInfo = getAuthorInfos(authorID);
                    urls = getAuthorUrls(authorID);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    authorInfo = new Model.AuthorInfo();
                    urls = Collections.emptyList();
                }

                mvc.model.storeAuthorInfos(authorInfo, urls);
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
    private Model.AuthorInfo getAuthorInfos(String authorID) throws Exception {
        String query = String.format(QUERIES[4], authorID);

        Log.d(TAG, query);

        return parseAuthorInfosXML(getXMLFromQuery(query));
    }

    private List<String> getAuthorUrls(String author_id) throws Exception {
        String query = String.format(QUERIES[5], author_id);

        Log.d(TAG, query);

        return parseAuthorUrlsXML(getXMLFromQuery(query));
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
    private Model.AuthorInfo parseAuthorInfosXML(String xml) throws Exception {
        Document document = buildDocumentXML(xml);

        NodeList nodeList;
        Element element;

        String profile_image_url, nsid, iconserver, iconfarm, username, realname, location, description;

        element = (Element) document.getElementsByTagName("person").item(0);
        nsid = element.getAttribute("nsid");
        iconserver = element.getAttribute("iconserver");
        iconfarm = element.getAttribute("iconfarm");
        if(Integer.parseInt(iconserver) > 0)
            profile_image_url = String.format(PROFILE_IMAGE_BASE_URL, iconfarm, iconserver, nsid);
        else
            profile_image_url = PROFILE_IMAGE_DEFAULT_URL;


        nodeList = document.getElementsByTagName("username");
        username = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("realname");
        realname = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("location");
        location = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        nodeList = document.getElementsByTagName("description");
        description = nodeList.getLength() != 0 ? nodeList.item(0).getTextContent() : "";

        return new Model.AuthorInfo(nsid, profile_image_url, username, realname, location, description);
    }

    @WorkerThread
    private List<String> parseAuthorUrlsXML(String xml) throws Exception {
        List<String> urls = new LinkedList<>();

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

            urls.add(String.format(IMAGE_BASE_URL, farm, server, id, secret, "q"));
        }

        return urls;
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
}