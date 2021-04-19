package com.parishod.watomatic.network;

import com.parishod.watomatic.model.GithubReleaseNotes;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetReleaseNotesService {
    @GET("/repos/adeekshith/watomatic/releases/latest")
    Call<GithubReleaseNotes> getReleaseNotes();
}
