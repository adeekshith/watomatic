package com.parishod.watomatic.network;

import com.parishod.watomatic.model.GithubReleaseNotes;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetReleaseNotesService {
    @GET("/repos/adeekshith/watomatic/releases")
    Call<List<GithubReleaseNotes>> getReleaseNotes();
}
