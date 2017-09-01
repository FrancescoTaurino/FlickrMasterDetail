package it.univr.francesco.flickr.model;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.util.LinkedList;
import java.util.List;

import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.view.View;

@ThreadSafe
public class Model {
    private MVC mvc;
    @GuardedBy("itself") private final LinkedList<PictureInfo> pictureInfos = new LinkedList<>();
    @GuardedBy("itself") private final LinkedList<AuthorInfo> authorInfos = new LinkedList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Immutable
    public static class PictureInfo {
        public final String pictureID;
        public final String authorID;
        public final String caption;
        public final String previewURL;
        public final String pictureURL;

        private final LinkedList<String> comments = new LinkedList<>();

        public PictureInfo(String pictureID, String authorID, String caption, String previewURL, String pictureURL) {
            this.pictureID = pictureID;
            this.authorID = authorID;
            this.caption = caption;
            this.previewURL = previewURL;
            this.pictureURL = pictureURL;
        }

        private PictureInfo() {
            this.pictureID = "";
            this.authorID = "";
            this.caption = "";
            this.previewURL = "";
            this.pictureURL = "";
        }

        public String[] getComments() {
            return this.comments.toArray(new String[this.comments.size()]);
        }
    }

    @Immutable
    public static class AuthorInfo {
        private final String authorID;
        public final String profile_image_url;
        public final String username;
        public final String realname;
        public final String location;
        public final String description;

        private final LinkedList<String> urls = new LinkedList<>();

        public AuthorInfo(String authorID, String profile_image_url, String username, String realname, String location, String description) {
            this.authorID = authorID;
            this.profile_image_url = profile_image_url;
            this.username = username;
            this.realname = realname;
            this.location = location;
            this.description = description;
        }

        public AuthorInfo() {
            this.authorID = "";
            this.profile_image_url = "";
            this.username = "";
            this.realname = "";
            this.location = "";
            this.description = "";
        }

        public String[] getUrls() {
            return this.urls.toArray(new String[this.urls.size()]);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void clearModel() {
        synchronized (this) {
            this.pictureInfos.clear();
            this.authorInfos.clear();
        }
        mvc.forEachView(View::onModelChanged);
    }

    public void storePictureInfos(List<PictureInfo> pictureInfos) {
        synchronized (this.pictureInfos) {
            this.pictureInfos.addAll(pictureInfos);
        }
        mvc.forEachView(View::onModelChanged);
    }

    public PictureInfo[] getPictureInfos() {
        synchronized (this.pictureInfos) {
            return this.pictureInfos.toArray(new PictureInfo[this.pictureInfos.size()]);
        }
    }

    public PictureInfo getPictureInfo(String pictureID) {
        synchronized (this.pictureInfos) {
            for(PictureInfo pictureInfo: this.pictureInfos)
                if(pictureInfo.pictureID.equals(pictureID))
                    return pictureInfo;

            return new PictureInfo();
        }
    }

    public void storeComments(String pictureID, List<String> comments) {
        synchronized (this.pictureInfos) {
            for(PictureInfo pictureInfo: this.pictureInfos) {
                if(pictureInfo.pictureID.equals(pictureID)) {
                    pictureInfo.comments.clear();
                    pictureInfo.comments.addAll(comments);

                    break;
                }
            }
        }
        mvc.forEachView(View::onModelChanged);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void storeAuthorInfos(AuthorInfo _authorInfo, List<String> urls) {
        synchronized (this.authorInfos) {
            for(AuthorInfo authorInfo: this.authorInfos) {
                if (authorInfo.authorID.equals(_authorInfo.authorID)) {
                    this.authorInfos.remove(authorInfo);

                    break;
                }
            }

            _authorInfo.urls.addAll(urls);
            this.authorInfos.add(_authorInfo);
        }

        mvc.forEachView(View::onModelChanged);
    }

    public AuthorInfo getAuthorInfo(String authorID) {
        synchronized (this.authorInfos) {
            for(AuthorInfo authorInfo: this.authorInfos)
                if(authorInfo.authorID.equals(authorID))
                    return authorInfo;

            return new AuthorInfo();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setMVC(MVC mvc) {
        this.mvc = mvc;
    }
}
