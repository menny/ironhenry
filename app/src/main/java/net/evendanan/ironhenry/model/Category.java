package net.evendanan.ironhenry.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

public class Category implements Parcelable {

    @SerializedName("ID")
    public final int ID;
    @SerializedName("name")
    public final String name;
    @SerializedName("slug")
    public final String slug;
    @SerializedName("description")
    public final String htmlDescription;
    @SerializedName("link")
    public final String link;
    @SerializedName("count")
    public final int count;
    @SerializedName("parent")
    @Nullable
    private final Object mParent;

    public Category(int id, String name, String slug, String htmlDescription, String link, int count, @Nullable Object parent) {
        ID = id;
        this.name = name;
        this.slug = slug;
        this.htmlDescription = htmlDescription;
        this.link = link;
        this.count = count;
        mParent = parent;
    }

    public boolean isRootCategory() {
        return mParent == null;
    }

    protected Category(Parcel in) {
        ID = in.readInt();
        name = in.readString();
        slug = in.readString();
        htmlDescription = in.readString();
        link = in.readString();
        count = in.readInt();
        mParent = in.readInt() == 0? null : new Object();
    }

    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ID);
        dest.writeString(name);
        dest.writeString(slug);
        dest.writeString(htmlDescription);
        dest.writeString(link);
        dest.writeInt(count);
        dest.writeInt(isRootCategory()? 0 : 1);
    }
}
