package net.evendanan.ironhenry.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class FeatureImage implements Parcelable {
    @SerializedName("ID")
    public final long ID;
    @SerializedName("title")
    public final String title;
    @SerializedName("source")
    public final String source;

    public FeatureImage(long id, String title, String source) {
        ID = id;
        this.title = title;
        this.source = source;
    }

    protected FeatureImage(Parcel in) {
        ID = in.readLong();
        title = in.readString();
        source = in.readString();
    }

    public static final Creator<FeatureImage> CREATOR = new Creator<FeatureImage>() {
        @Override
        public FeatureImage createFromParcel(Parcel in) {
            return new FeatureImage(in);
        }

        @Override
        public FeatureImage[] newArray(int size) {
            return new FeatureImage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(ID);
        dest.writeString(title);
        dest.writeString(source);
    }
}
