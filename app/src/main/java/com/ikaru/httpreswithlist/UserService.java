package com.ikaru.httpreswithlist;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UserService {
    String BASE_URL = "https://5d98133b61c84c00147d6d4d.mockapi.io/";

    @GET("/User")
    Observable<List<User>> getUsers();
}
