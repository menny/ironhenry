package net.evendanan.ironhenry.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.ArrayMap;
import android.util.SparseArray;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Posts implements Parcelable {

    @SerializedName("postsMap")
    public final Map<String, List<Post>> postsMap;
    private static final Comparator<? super Post> msPostsIdComparator = (lhs, rhs) -> rhs.ID - lhs.ID;

    public Posts() {
        postsMap = new ArrayMap<>();
    }

    protected Posts(Parcel in) {
        postsMap = new ArrayMap<>();
        in.readMap(postsMap, Posts.class.getClassLoader());
    }

    public List<Post> addPosts(String slug, Post[] newPosts) {
        List<Post> posts = postsMap.containsKey(slug)? postsMap.get(slug) : new ArrayList<>();
        postsMap.put(slug, posts);
        //yes, I know it dumb. I'll optimize it later
        SparseArray<Post> sparseArray = new SparseArray<>(newPosts.length + posts.size());
        for (Post newPost : newPosts) {
            sparseArray.put(newPost.ID, newPost);
        }

        for (Post oldPost : posts) {
            if (sparseArray.indexOfKey(oldPost.ID) < 0) {
                //key not found (backend may return only newer posts then what we have)
                sparseArray.put(oldPost.ID, oldPost);
            }
        }

        posts.clear();
        for (int arrayKeyIndex=0; arrayKeyIndex<sparseArray.size(); arrayKeyIndex++) {
            posts.add(sparseArray.valueAt(arrayKeyIndex));
        }
        Collections.sort(posts, msPostsIdComparator);

        return posts;
    }

    public static final Creator<Posts> CREATOR = new Creator<Posts>() {
        @Override
        public Posts createFromParcel(Parcel in) {
            return new Posts(in);
        }

        @Override
        public Posts[] newArray(int size) {
            return new Posts[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(postsMap);
    }
}
