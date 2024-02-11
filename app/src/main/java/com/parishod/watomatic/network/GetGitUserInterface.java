package com.parishod.watomatic.network;

import com.parishod.watomatic.model.githubUesrsResponse.GithubUsersResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetGitUserInterface {
    @GET("/repos/adeekshith/watomatic/contributors")
    Call<List<GithubUsersResponse>> getContriUsers();

}
