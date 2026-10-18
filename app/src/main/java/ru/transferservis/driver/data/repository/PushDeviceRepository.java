package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.StaffApi;
import ru.transferservis.driver.data.model.BasicResponse;
import ru.transferservis.driver.data.model.PushDeviceRegistrationRequest;
import ru.transferservis.driver.data.model.PushDeviceUnregistrationRequest;

public final class PushDeviceRepository {

    public interface Callback {

        void onSuccess();

        void onError(
                int httpStatus
        );
    }

    private static final String PLATFORM_ANDROID =
            "android";

    private final StaffApi staffApi;

    public PushDeviceRepository() {

        staffApi =
                ApiClient.getStaffApi();
    }

    public void register(
            String installationId,
            String token,
            Callback callback
    ) {

        if (callback == null) {
            return;
        }

        String installation =
                clean(
                        installationId
                );

        String fcmToken =
                clean(
                        token
                );

        if (
                installation.isEmpty()
                        ||
                        fcmToken.isEmpty()
        ) {

            callback.onError(
                    0
            );

            return;
        }

        PushDeviceRegistrationRequest request =
                new PushDeviceRegistrationRequest(
                        installation,
                        PLATFORM_ANDROID,
                        fcmToken
                );

        staffApi
                .registerPushDevice(
                        request
                )
                .enqueue(
                        callbackAdapter(
                                callback
                        )
                );
    }

    public void unregister(
            String installationId,
            Callback callback
    ) {

        if (callback == null) {
            return;
        }

        String installation =
                clean(
                        installationId
                );

        if (installation.isEmpty()) {

            callback.onSuccess();

            return;
        }

        PushDeviceUnregistrationRequest request =
                new PushDeviceUnregistrationRequest(
                        installation
                );

        staffApi
                .unregisterPushDevice(
                        request
                )
                .enqueue(
                        callbackAdapter(
                                callback
                        )
                );
    }

    private Callback<BasicResponse> callbackAdapter(
            Callback callback
    ) {

        return new Callback<BasicResponse>() {

            @Override
            public void onResponse(
                    @NonNull Call<BasicResponse> call,
                    @NonNull Response<BasicResponse> response
            ) {

                if (
                        !response.isSuccessful()
                ) {

                    callback.onError(
                            response.code()
                    );

                    return;
                }

                BasicResponse body =
                        response.body();

                if (
                        body == null
                                ||
                                !body.isOk()
                ) {

                    callback.onError(
                            response.code()
                    );

                    return;
                }

                callback.onSuccess();
            }

            @Override
            public void onFailure(
                    @NonNull Call<BasicResponse> call,
                    @NonNull Throwable throwable
            ) {

                if (
                        call.isCanceled()
                ) {

                    return;
                }

                callback.onError(
                        0
                );
            }
        };
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
