package com.parishod.watomatic.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface GetDonationsProgressService {
    @GET
    Call<String> getDonationProgress(@Url String url);
}
