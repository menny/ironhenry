package net.evendanan.ironhenry.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Categories implements Parcelable {

    @SerializedName("categories")
    public final List<Category> categories;

    public Categories() {
        categories = new ArrayList<>();
    }

    protected Categories(Parcel in) {
        categories = new ArrayList<>();
        in.readList(categories, Categories.class.getClassLoader());
    }

    public static final Creator<Categories> CREATOR = new Creator<Categories>() {
        @Override
        public Categories createFromParcel(Parcel in) {
            return new Categories(in);
        }

        @Override
        public Categories[] newArray(int size) {
            return new Categories[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(categories);
    }
}
