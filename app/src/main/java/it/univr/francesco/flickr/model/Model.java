package it.univr.francesco.flickr.model;

import android.graphics.Bitmap;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.view.View;

@ThreadSafe
public class Model {
    public final static int PICTURES_LIST_NUMBER = 25;
    public final static int PICTURES_AUTHOR_NUMBER = 12;

    public final static String PICTURE_SMALL = "small";
    public final static String PICTURE_LARGE = "large";

    public final static String URL = "url";
    public final static String PIC = "pic";

    private MVC mvc;
    @GuardedBy("itself") private final PictureInfo[] pictureInfos = new PictureInfo[PICTURES_LIST_NUMBER];
    @GuardedBy("itself") private final Picture[] pictureCache = new Picture[PICTURES_LIST_NUMBER];
    @GuardedBy("itself") private final AtomicInteger lastPictureOpened = new AtomicInteger(-1);
    @GuardedBy("itself") private final AuthorInfo authorInfo = new AuthorInfo();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Immutable
    public static class PictureInfo {
        public final String picture_id;
        private final String title;
        public final String author_id;
        private final String author_name;
        public final String previewURL;
        public final String pictureURL;

        private final LinkedList<Comment> comments = new LinkedList<>();

        @Immutable
        public static class Comment {
            private final String author;
            private final String text;

            public Comment(String author, String text) {
                this.author = author;
                this.text = text;
            }

            public String toFormattedString() {
                return String.format("<b><font color='black'>%s: </font></b>%s", author, text);
            }
        }

        public PictureInfo(String picture_id, String title, String author_id, String author_name, String previewURL, String pictureURL) {
            this.picture_id = picture_id;
            this.title = title;
            this.author_id = author_id;
            this.author_name = author_name;
            this.previewURL = previewURL;
            this.pictureURL = pictureURL;
        }

        public String toFormattedString() {
            return String.format("<b>%s</b><br><br><font color=\"gray\">%s</font>", title, author_name);
        }
    }

    @Immutable
    private class Picture {
        private final Bitmap small;
        private final Bitmap large;

        private Picture(Bitmap small, Bitmap large) {
            this.small = small;
            this.large = large;
        }
    }

    @ThreadSafe
    public static class AuthorInfo {
        private AuthorInfoGeneral general;
        private final RecentUpload[] recentUploads = new RecentUpload[PICTURES_AUTHOR_NUMBER];

        @Immutable
        public static class AuthorInfoGeneral {
            public final Bitmap profile_image;
            public final String username;
            public final String realname;
            public final String location;
            public final String description;

            public AuthorInfoGeneral(Bitmap profile_image, String username, String realname, String location, String description) {
                this.profile_image = profile_image;
                this.username = username;
                this.realname = realname;
                this.location = location;
                this.description = description;
            }
        }

        private static class RecentUpload {
            private final String URL;
            private final Bitmap pic;

            private RecentUpload(String URL, Bitmap pic) {
                this.URL = URL;
                this.pic = pic;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearModel() {
        synchronized (this.pictureInfos) {
            Arrays.fill(this.pictureInfos, null);
        }

        synchronized (this.pictureCache) {
            Arrays.fill(this.pictureCache, null);
        }

        lastPictureOpened.set(-1);

        mvc.forEachView(View::onModelChanged);
    }

    public void storePictureInfos(PictureInfo[] pictureInfos) {
        synchronized (this.pictureInfos) {
            System.arraycopy(pictureInfos, 0, this.pictureInfos, 0, PICTURES_LIST_NUMBER);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public PictureInfo[] getPictureInfos() {
        synchronized (this.pictureInfos) {
            return this.pictureInfos.clone();
        }
    }

    public PictureInfo getPictureInfoAtPosition(int position) {
        synchronized (this.pictureInfos) {
            return this.pictureInfos[position];
        }
    }

    public void addPictureToCache(int position, Bitmap picture, String type) {
        synchronized (this.pictureCache) {
            Picture tmpPicture = this.pictureCache[position];
            Picture newPicture;

            if(type.equals(PICTURE_SMALL)) newPicture = new Picture(picture, tmpPicture == null ? null : tmpPicture.large);
            else newPicture = new Picture(tmpPicture == null ? null : tmpPicture.small, picture);

            pictureCache[position] = newPicture;
        }

        mvc.forEachView(View::onModelChanged);
    }

    public Bitmap getPictureFromCache(int position, String type) {
        synchronized (this.pictureCache) {
            Picture tmpPicture = this.pictureCache[position];

            if(type.equals(PICTURE_SMALL) && tmpPicture != null) return tmpPicture.small;
            else if(type.equals(PICTURE_LARGE) && tmpPicture != null) return tmpPicture.large;
            else return null;
        }
    }

    public void storeCommentsOfPictureInfoAtPosition(int position, List<PictureInfo.Comment> comments) {
        synchronized (this.pictureInfos) {
            this.pictureInfos[position].comments.clear();
            this.pictureInfos[position].comments.addAll(comments);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public PictureInfo.Comment[] getCommentsOfPictureInfoAtPosition(int position) {
        synchronized (this.pictureInfos) {
            int commentsListSize = this.pictureInfos[position].comments.size();

            return this.pictureInfos[position].comments.toArray(new PictureInfo.Comment[commentsListSize]);
        }
    }

    public void setLastPictureOpened(int position) {
        this.lastPictureOpened.set(position);
    }

    public int getLastPictureOpened() {
        return this.lastPictureOpened.get();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearAuthorInfo() {
        synchronized (this.authorInfo) {
            this.authorInfo.general = null;
            Arrays.fill(this.authorInfo.recentUploads, null);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public void storeAuthorInfoGeneral(AuthorInfo.AuthorInfoGeneral authorInfoGeneral) {
        synchronized (this.authorInfo) {
            this.authorInfo.general = authorInfoGeneral;
        }

        mvc.forEachView(View::onModelChanged);
    }

    public AuthorInfo.AuthorInfoGeneral getAuthorInfoGeneral() {
        synchronized (this.authorInfo) {
            if(this.authorInfo.general == null)
                return null;

            return new AuthorInfo.AuthorInfoGeneral(
                    this.authorInfo.general.profile_image,
                    this.authorInfo.general.username,
                    this.authorInfo.general.realname,
                    this.authorInfo.general.location,
                    this.authorInfo.general.description);
        }
    }

    public void addURLsToRecentUploads(String[] URLs) {
        synchronized (this.authorInfo) {
            for(int i = 0; i < PICTURES_AUTHOR_NUMBER; i++)
                this.authorInfo.recentUploads[i] = new AuthorInfo.RecentUpload(URLs[i], null);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public String[] getURLsFromRecentUploads() {
        synchronized (this.authorInfo) {
            String[] URLs = new String[PICTURES_AUTHOR_NUMBER];

            if(this.authorInfo.recentUploads[0] == null)
                return URLs;

            for(int i = 0; i < PICTURES_AUTHOR_NUMBER; i++)
                URLs[i] = this.authorInfo.recentUploads[i].URL;

            return URLs;
        }
    }

    public void addPicToRecentUploads(int position, Bitmap pic) {
        synchronized (this.authorInfo) {
            this.authorInfo.recentUploads[position] = new AuthorInfo.RecentUpload(this.authorInfo.recentUploads[position].URL, pic);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public Object getFromRecentUploadsAtPosition(int position, String type) {
        synchronized (this.authorInfo) {
            AuthorInfo.RecentUpload recentUpload = this.authorInfo.recentUploads[position];

            if(type.equals(URL) && recentUpload != null) return recentUpload.URL;
            else if (type.equals(PIC) && recentUpload != null) return this.authorInfo.recentUploads[position].pic;
            else return null;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setMVC(MVC mvc) {
        this.mvc = mvc;
    }
}
