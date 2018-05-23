package com.project.safereturn;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by thstj on 2017-09-25.
 */

public interface Http {

    /*
    get parameter는 @query로 보내야 합니다. language는 선택적 파라미터로 받고싶은 주소 언어를 선택할 수 있습니다.
     */
    @GET("json")
    Call<JsonObject> getAddress(@Query("latlng") String latlng, @Query("api") String api, @Query("language") String language);

}
