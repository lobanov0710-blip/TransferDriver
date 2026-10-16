package ru.transferservis.driver.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import ru.transferservis.driver.data.model.BasicResponse;
import ru.transferservis.driver.data.model.LoginRequest;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.model.StaffRefreshTokenRequest;

public interface StaffApi {

    @POST("staff/login")
    Call<StaffLoginResponse> login(
            @Body LoginRequest request
    );

    @POST("staff/refresh")
    Call<StaffLoginResponse> refresh(
            @Body StaffRefreshTokenRequest request
    );

    @POST("staff/logout")
    Call<BasicResponse> logout(
            @Body StaffRefreshTokenRequest request
    );
}