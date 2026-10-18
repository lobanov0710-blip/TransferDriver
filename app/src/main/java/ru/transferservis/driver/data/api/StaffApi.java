package ru.transferservis.driver.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;

import ru.transferservis.driver.data.model.BasicResponse;
import ru.transferservis.driver.data.model.LoginRequest;
import ru.transferservis.driver.data.model.PushDeviceRegistrationRequest;
import ru.transferservis.driver.data.model.PushDeviceUnregistrationRequest;
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

    @Headers("X-Staff-Auth: true")
    @PUT("staff/push-device")
    Call<BasicResponse> registerPushDevice(
            @Body
            PushDeviceRegistrationRequest request
    );

    @Headers("X-Staff-Auth: true")
    @HTTP(
            method = "DELETE",
            path = "staff/push-device",
            hasBody = true
    )
    Call<BasicResponse> unregisterPushDevice(
            @Body
            PushDeviceUnregistrationRequest request
    );
}
