package ru.transferservis.driver.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.transferservis.driver.data.model.DriverAssignmentsResponse;
import ru.transferservis.driver.data.model.OrderStatusRequest;
import ru.transferservis.driver.data.model.OrderTransitionResponse;

public interface OrderApi {

    @Headers("X-Staff-Auth: true")
    @GET("v1/driver/assignments")
    Call<DriverAssignmentsResponse> getAssignments(
            @Query("limit") int limit
    );

    @Headers("X-Staff-Auth: true")
    @POST("orders/{orderId}/status")
    Call<OrderTransitionResponse> changeOrderStatus(
            @Path(
                    value = "orderId",
                    encoded = false
            )
            String orderId,

            @Header("Idempotency-Key")
            String idempotencyKey,

            @Body
            OrderStatusRequest request
    );
}