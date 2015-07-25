package net.evendanan.ironhenry.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

public class OfflineState implements Parcelable {
    public static final float PROGRESS_ERROR = -1f;
    public static final float PROGRESS_NONE = 0f;
    public static final float PROGRESS_FULL = 1f;

    public final Post post;
    private float mDownloadProgress;

    public OfflineState(@NonNull Post post) {
        this.post = Preconditions.checkNotNull(post);
        setDownloadProgress(0f);
    }

    public float getDownloadProgress() {
        return mDownloadProgress;
    }

    public void setDownloadProgress(float downloadProgress){
        Preconditions.checkArgument(downloadProgress >= PROGRESS_NONE || downloadProgress == PROGRESS_ERROR);
        Preconditions.checkArgument(downloadProgress <= PROGRESS_FULL);
        this.mDownloadProgress = downloadProgress;

    }

    protected OfflineState(Parcel in) {
        post = in.readParcelable(Post.class.getClassLoader());
        mDownloadProgress = in.readFloat();
    }

    public static final Creator<OfflineState> CREATOR = new Creator<OfflineState>() {
        @Override
        public OfflineState createFromParcel(Parcel in) {
            return new OfflineState(in);
        }

        @Override
        public OfflineState[] newArray(int size) {
            return new OfflineState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(post, flags);
        dest.writeFloat(mDownloadProgress);
    }

    @Override
    public int hashCode() {
        return post.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflineState && ((OfflineState)o).post.equals(post);
    }
}
