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

    private MVC mvc;
    @GuardedBy("itself") private final PictureInfo[] pictureInfos = new PictureInfo[PICTURES_LIST_NUMBER];
    @GuardedBy("itself") private final AuthorInfo authorInfo = new AuthorInfo();
    @GuardedBy("itself") public final AtomicInteger lastPictureOpened = new AtomicInteger(-1);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Immutable
    public static class PictureInfo {
        public final String pictureID;
        public final String authorID;
        public final String caption;
        public final String previewURL;
        public final String pictureURL;

        private final Bitmap[] pictures = new Bitmap[2]; // small (preview) and large (hd) picture
        private final LinkedList<String> comments = new LinkedList<>();

        public PictureInfo(String pictureID, String authorID, String caption, String previewURL, String pictureURL) {
            this.pictureID = pictureID;
            this.authorID = authorID;
            this.caption = caption;
            this.previewURL = previewURL;
            this.pictureURL = pictureURL;
        }
    }

    @ThreadSafe
    public static class AuthorInfo {
        private AuthorInfoGeneral general;
        private final String[] URLs = new String[PICTURES_AUTHOR_NUMBER];
        private final Bitmap[] pics = new Bitmap[PICTURES_AUTHOR_NUMBER];

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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearPictureInfos() {
        synchronized (this.pictureInfos) {
            Arrays.fill(this.pictureInfos, null);
        }

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

    public boolean pictureInfosIsReady() {
        synchronized (this.pictureInfos) {
            return this.pictureInfos[0] != null;
        }
    }

    public PictureInfo getPictureInfoAtPosition(int position) {
        synchronized (this.pictureInfos) {
            return this.pictureInfos[position];
        }
    }

    public void storePictureOfPictureInfoAtPosition(int position, Bitmap picture, String type) {
        synchronized (this.pictureInfos) {
            switch (type) {
                case PICTURE_SMALL:
                    if(this.pictureInfos[position] != null) this.pictureInfos[position].pictures[0] = picture;
                    break;
                case PICTURE_LARGE:
                    if(this.pictureInfos[position] != null) this.pictureInfos[position].pictures[1] = picture;
                    break;
            }
        }

        mvc.forEachView(View::onModelChanged);
    }

    public Bitmap getPictureOfPictureInfoAtPosition(int position, String type) {
        synchronized (this.pictureInfos) {
            switch (type) {
                case PICTURE_SMALL:
                    return this.pictureInfos[position].pictures[0];
                case PICTURE_LARGE:
                    return this.pictureInfos[position].pictures[1];
            }
            return null;
        }
    }

    public void storeCommentsOfPictureInfoAtPosition(int position, List<String> comments) {
        synchronized (this.pictureInfos) {
            if(this.pictureInfos[position] != null) {
                this.pictureInfos[position].comments.clear();
                this.pictureInfos[position].comments.addAll(comments);
            }
        }

        mvc.forEachView(View::onModelChanged);
    }

    public String[] getCommentsOfPictureInfoAtPosition(int position) {
        synchronized (this.pictureInfos) {
            return this.pictureInfos[position].comments
                    .toArray(new String[this.pictureInfos[position].comments.size()]);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearAuthorInfo() {
        synchronized (this.authorInfo) {
            this.authorInfo.general = null;
            Arrays.fill(this.authorInfo.URLs, null);
            Arrays.fill(this.authorInfo.pics, null);
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

    public void storeURLsOfAuthorInfo(String[] URLs) {
        synchronized (this.authorInfo) {
            System.arraycopy(URLs, 0, this.authorInfo.URLs, 0, PICTURES_AUTHOR_NUMBER);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public String[] getURLsFromAuthorInfo() {
        synchronized (this.authorInfo) {
            return this.authorInfo.URLs.clone();
        }
    }

    public void storePicOfAuthorInfoAtPosition(int position, Bitmap bitmap) {
        synchronized (this.authorInfo) {
            this.authorInfo.pics[position] = bitmap;
        }

        mvc.forEachView(View::onModelChanged);
    }

    public Bitmap getPicsFromAuthorInfoAtPosition(int position) {
        synchronized (this.authorInfo) {
            return this.authorInfo.pics[position];
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setMVC(MVC mvc) {
        this.mvc = mvc;
    }
}
