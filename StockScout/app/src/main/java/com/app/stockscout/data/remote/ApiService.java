package com.app.stockscout.data.remote;

import com.app.stockscout.data.model.Item;
import com.app.stockscout.data.model.SyncPickRequest;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // This will call: https://69f8dcacf7044aa0103e9401.mockapi.io/items
    @GET("items")
    Call<List<Item>> getItems();

    @POST("picks")
    Call<Void> recordPick(@Body SyncPickRequest pickRequest);
}