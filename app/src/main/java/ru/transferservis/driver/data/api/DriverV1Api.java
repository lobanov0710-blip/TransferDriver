package ru.transferservis.driver.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Url;

import okhttp3.RequestBody;

import ru.transferservis.driver.data.model.DriverPhotoChecksResponse;
import ru.transferservis.driver.data.model.DriverUploadIssueRequest;
import ru.transferservis.driver.data.model.DriverUploadIssueResponse;
import ru.transferservis.driver.data.model.PrivateUploadResponse;
import ru.transferservis.driver.data.model.SupportCaseCreateRequest;
import ru.transferservis.driver.data.model.SupportCaseResponse;
import ru.transferservis.driver.data.model.SupportCasesResponse;
import ru.transferservis.driver.data.model.SupportMessageMutationResponse;
import ru.transferservis.driver.data.model.SupportMessageRequest;

public interface DriverV1Api {

    @Headers("X-Staff-Auth: true")
    @GET("v1/driver/photo-checks")
    Call<DriverPhotoChecksResponse> getPhotoChecks();


    @Headers("X-Staff-Auth: true")
    @GET("v1/driver/support-cases")
    Call<SupportCasesResponse> getSupportCases(
            @Query("limit")
            int limit
    );


    @Headers("X-Staff-Auth: true")
    @POST("v1/driver/support-cases")
    Call<SupportCaseResponse> createSupportCase(
            @Header("Idempotency-Key")
            String idempotencyKey,

            @Body
            SupportCaseCreateRequest request
    );


    @Headers("X-Staff-Auth: true")
    @GET("v1/driver/support-cases/{caseId}")
    Call<SupportCaseResponse> getSupportCase(
            @Path(
                    value = "caseId",
                    encoded = false
            )
            String caseId
    );


    @Headers("X-Staff-Auth: true")
    @POST("v1/driver/support-cases/{caseId}/messages")
    Call<SupportMessageMutationResponse> sendSupportMessage(
            @Path(
                    value = "caseId",
                    encoded = false
            )
            String caseId,

            @Header("Idempotency-Key")
            String idempotencyKey,

            @Body
            SupportMessageRequest request
    );


    @Headers("X-Staff-Auth: true")
    @POST("v1/driver/uploads")
    Call<DriverUploadIssueResponse> issuePrivateUpload(
            @Body
            DriverUploadIssueRequest request
    );


    @Headers("X-Staff-Auth: true")
    @PUT
    Call<PrivateUploadResponse> uploadPrivateResource(
            @Url
            String relativePath,

            @Header("Content-Type")
            String contentType,

            @Body
            RequestBody body
    );
}
