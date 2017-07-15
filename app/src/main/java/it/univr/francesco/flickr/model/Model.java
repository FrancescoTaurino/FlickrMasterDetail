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
    @GuardedBy("itself") private final LinkedList<PictureInfo> pictureInfos = new LinkedList<>();
    @GuardedBy("itself") private final Author author = new Author();
    @GuardedBy("itself") public final AtomicInteger lastQueryID = new AtomicInteger(0);

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

        public Bitmap getPicture(String type) {
            return this.pictures[type.equals(PICTURE_SMALL) ? 0 : 1];
        }

        public String[] getComments() {
            return this.comments.toArray(new String[this.comments.size()]);
        }
    }

    @ThreadSafe
    public static class Author {
        private AuthorInfo authorInfo;
        private final String[] URLs = new String[PICTURES_AUTHOR_NUMBER];
        private final Bitmap[] pics = new Bitmap[PICTURES_AUTHOR_NUMBER];

        @Immutable
        public static class AuthorInfo {
            public final Bitmap profile_image;
            public final String username;
            public final String realname;
            public final String location;
            public final String description;

            public AuthorInfo(Bitmap profile_image, String username, String realname, String location, String description) {
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
            this.pictureInfos.clear();
        }
        mvc.forEachView(View::onModelChanged);
    }

    public void storePictureInfos(List<PictureInfo> pictureInfos, int lastQueryID) {
        synchronized (this.pictureInfos) {
            if(this.lastQueryID.get() == lastQueryID)
                this.pictureInfos.addAll(pictureInfos);
        }
        mvc.forEachView(View::onModelChanged);
    }

    public PictureInfo[] getPictureInfos() {
        synchronized (this.pictureInfos) {
            return this.pictureInfos.toArray(new PictureInfo[this.pictureInfos.size()]);
        }
    }

    public PictureInfo getPictureInfo(int position) {
        synchronized (this.pictureInfos) {
            if(this.pictureInfos.size() <= position)
                return null;

            return this.pictureInfos.get(position);
        }
    }

    public void storePicture(int position, Bitmap picture, String type, int lastQueryID) {
        synchronized (this.pictureInfos) {
            if(this.pictureInfos.size() <= position)
                return;

            switch (type) {
                case PICTURE_SMALL:
                    if(this.lastQueryID.get() == lastQueryID) this.pictureInfos.get(position).pictures[0] = picture;
                    break;
                case PICTURE_LARGE:
                    this.pictureInfos.get(position).pictures[1] = picture;
                    break;
            }
        }
        mvc.forEachView(View::onModelChanged);
    }

    public void storeComments(int position, List<String> comments) {
        synchronized (this.pictureInfos) {
            if(this.pictureInfos.size() <= position)
                return;

            this.pictureInfos.get(position).comments.clear();
            this.pictureInfos.get(position).comments.addAll(comments);
        }
        mvc.forEachView(View::onModelChanged);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearAuthor() {
        synchronized (this.author) {
            this.author.authorInfo = null;
            Arrays.fill(this.author.URLs, null);
            Arrays.fill(this.author.pics, null);
        }
        mvc.forEachView(View::onModelChanged);
    }

    public void storeAuthorInfo(Author.AuthorInfo authorInfo) {
        synchronized (this.author) {
            this.author.authorInfo = authorInfo;
        }

        mvc.forEachView(View::onModelChanged);
    }

    public Author.AuthorInfo getAuthorInfo() {
        synchronized (this.author) {
            if(this.author.authorInfo == null)
                return null;

            return new Author.AuthorInfo(
                    this.author.authorInfo.profile_image,
                    this.author.authorInfo.username,
                    this.author.authorInfo.realname,
                    this.author.authorInfo.location,
                    this.author.authorInfo.description);
        }
    }

    public void storeAuthorURLs(String[] URLs) {
        synchronized (this.author) {
            System.arraycopy(URLs, 0, this.author.URLs, 0, PICTURES_AUTHOR_NUMBER);

        }
        mvc.forEachView(View::onModelChanged);
    }

    public String[] getAuthorURLs() {
        synchronized (this.author) {
            return this.author.URLs.clone();
        }
    }

    public String getAuthorURL(int position) {
        synchronized (this.author) {
            return this.author.URLs[position];
        }
    }

    public void storeAuthorPic(int position, Bitmap bitmap) {
        synchronized (this.author) {
            this.author.pics[position] = bitmap;
        }
        mvc.forEachView(View::onModelChanged);
    }

    public Bitmap getAuthorPic(int position) {
        synchronized (this.author) {
            return this.author.pics[position];
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setMVC(MVC mvc) {
        this.mvc = mvc;
    }
}
