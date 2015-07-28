package net.evendanan.ironhenry.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Post implements Parcelable {
    private static final Pattern msLinkPattern = Pattern.compile("href=\"(http[^\"]*.mp3)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @SerializedName("ID")
    public final int ID;
    @SerializedName("title")
    public final String title;
    @SerializedName("content")
    public final String htmlContent;
    @SerializedName("link")
    public final String link;
    @SerializedName("modified")
    public final String modified;
    @SerializedName("excerpt")
    public final String excerpt;
    @SerializedName("featured_image")
    @Nullable
    public final FeatureImage featuredImage;

    public Post(int id, String title, String htmlContent, String link, String modified, String excerpt, @Nullable FeatureImage featuredImage) {
        ID = id;
        this.title = title;
        this.htmlContent = htmlContent;
        this.link = link;
        this.modified = modified;
        this.excerpt = excerpt;
        this.featuredImage = featuredImage;
    }

    private Post(Parcel in) {
        ID = in.readInt();
        title = in.readString();
        htmlContent = in.readString();
        link = in.readString();
        modified = in.readString();
        excerpt = in.readString();
        featuredImage = in.readParcelable(FeatureImage.class.getClassLoader());
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ID);
        dest.writeString(title);
        dest.writeString(htmlContent);
        dest.writeString(link);
        dest.writeString(modified);
        dest.writeString(excerpt);
        dest.writeParcelable(featuredImage, 0);
    }

    @Nullable
    public Uri extractStoryAudioLink() {
        if (TextUtils.isEmpty(htmlContent)) {
            return null;
        }
        Matcher pageMatcher = msLinkPattern.matcher(htmlContent);
        while (pageMatcher.find()) {
            if (pageMatcher.groupCount() == 1) return Uri.parse(pageMatcher.group(1));
        }

        return null;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Post && ((Post)o).ID == ID;
    }
}
