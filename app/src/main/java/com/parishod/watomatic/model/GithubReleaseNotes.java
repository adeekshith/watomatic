package com.parishod.watomatic.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GithubReleaseNotes implements Parcelable {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("tag_name")
    @Expose
    private String tagName;
    @SerializedName("body")
    @Expose
    private String body;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.tagName);
        dest.writeString(this.body);
    }

    public void readFromParcel(Parcel source) {
        this.id = (Integer) source.readValue(Integer.class.getClassLoader());
        this.tagName = source.readString();
        this.body = source.readString();
    }

    public GithubReleaseNotes() {
    }

    protected GithubReleaseNotes(Parcel in) {
        this.id = (Integer) in.readValue(Integer.class.getClassLoader());
        this.tagName = in.readString();
        this.body = in.readString();
    }

    public static final Parcelable.Creator<GithubReleaseNotes> CREATOR = new Parcelable.Creator<GithubReleaseNotes>() {
        @Override
        public GithubReleaseNotes createFromParcel(Parcel source) {
            return new GithubReleaseNotes(source);
        }

        @Override
        public GithubReleaseNotes[] newArray(int size) {
            return new GithubReleaseNotes[size];
        }
    };
}
