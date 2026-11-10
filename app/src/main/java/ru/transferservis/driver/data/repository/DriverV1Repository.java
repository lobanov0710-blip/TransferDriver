package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.DriverV1Api;
import ru.transferservis.driver.data.api.StaffAuthInterceptor;
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;
import ru.transferservis.driver.data.model.DriverPhotoChecksResponse;
import ru.transferservis.driver.data.model.DriverUploadIssueRequest;
import ru.transferservis.driver.data.model.DriverUploadIssueResponse;
import ru.transferservis.driver.data.model.DriverUploadSessionDto;
import ru.transferservis.driver.data.model.PrivateResourceDto;
import ru.transferservis.driver.data.model.PrivateUploadResponse;
import ru.transferservis.driver.data.model.SupportCaseCreateRequest;
import ru.transferservis.driver.data.model.SupportCaseDto;
import ru.transferservis.driver.data.model.SupportCaseResponse;
import ru.transferservis.driver.data.model.SupportCasesResponse;
import ru.transferservis.driver.data.model.SupportMessageMutationResponse;
import ru.transferservis.driver.data.model.SupportMessageRequest;

public final class DriverV1Repository implements SupportCaseGateway, PhotoCheckGateway {

    public interface PhotoChecksCallback {

        void onSuccess(
                List<DriverPhotoCheckDto> checks
        );

        void onAuthenticationRequired();

        void onError(
                int httpStatus,
                String message
        );
    }

    public interface SupportCasesCallback {

        void onSuccess(
                List<SupportCaseDto> cases
        );

        void onAuthenticationRequired();

        void onError(
                int httpStatus,
                String message
        );
    }

    public interface SupportCaseCallback {

        void onSuccess(
                SupportCaseDto supportCase
        );

        void onAuthenticationRequired();

        void onConflict();

        void onError(
                int httpStatus,
                String message
        );
    }

    public interface UploadCallback {

        void onSuccess(
                PrivateResourceDto resource
        );

        void onAuthenticationRequired();

        void onConflict();

        void onError(
                int httpStatus,
                String message
        );
    }

    private final DriverV1Api api;

    public DriverV1Repository() {
        this(ApiClient.getDriverV1Api());
    }

    // Package-private seam for isolated contract tests; production uses ApiClient.
    DriverV1Repository(DriverV1Api api) {
        if (api == null) {
            throw new IllegalArgumentException("DriverV1Api required");
        }
        this.api = api;
    }

    public void getPhotoChecks(
            PhotoChecksCallback callback
    ) {

        if (callback == null) {
            return;
        }

        api
                .getPhotoChecks()
                .enqueue(
                        new Callback<DriverPhotoChecksResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<DriverPhotoChecksResponse> call,
                                    @NonNull Response<DriverPhotoChecksResponse> response
                            ) {

                                if (
                                        handlePhotoAuth(
                                                response,
                                                callback
                                        )
                                ) {
                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapReadError(
                                                    response.code(),
                                                    "Не удалось загрузить фотоконтроль"
                                            )
                                    );

                                    return;
                                }

                                DriverPhotoChecksResponse body =
                                        response.body();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                List<DriverPhotoCheckDto> checks =
                                        body.getPhotoChecks();

                                callback.onSuccess(
                                        checks == null
                                                ? Collections.emptyList()
                                                : checks
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<DriverPhotoChecksResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось загрузить фотоконтроль. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    public void getSupportCases(
            int limit,
            SupportCasesCallback callback
    ) {

        if (callback == null) {
            return;
        }

        int safeLimit =
                Math.max(
                        1,
                        Math.min(
                                limit,
                                200
                        )
                );

        api
                .getSupportCases(
                        safeLimit
                )
                .enqueue(
                        new Callback<SupportCasesResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<SupportCasesResponse> call,
                                    @NonNull Response<SupportCasesResponse> response
                            ) {

                                if (
                                        handleSupportListAuth(
                                                response,
                                                callback
                                        )
                                ) {
                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapReadError(
                                                    response.code(),
                                                    "Не удалось загрузить обращения"
                                            )
                                    );

                                    return;
                                }

                                SupportCasesResponse body =
                                        response.body();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                List<SupportCaseDto> cases =
                                        body.getSupportCases();

                                callback.onSuccess(
                                        cases == null
                                                ? Collections.emptyList()
                                                : cases
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<SupportCasesResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось загрузить обращения. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    public void getSupportCase(
            String caseId,
            SupportCaseCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String safeCaseId =
                clean(caseId);

        if (safeCaseId.isEmpty()) {

            callback.onError(
                    0,
                    "Некорректный ID обращения"
            );

            return;
        }

        api
                .getSupportCase(
                        safeCaseId
                )
                .enqueue(
                        supportCaseResponseCallback(
                                callback,
                                false
                        )
                );
    }

    public void createSupportCase(
            String category,
            String orderId,
            String idempotencyKey,
            SupportCaseCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String safeCategory =
                clean(category);

        String safeKey =
                clean(idempotencyKey);

        if (
                safeCategory.isEmpty()
                        ||
                        safeKey.isEmpty()
        ) {

            callback.onError(
                    0,
                    "Некорректные параметры обращения"
            );

            return;
        }

        api
                .createSupportCase(
                        safeKey,
                        new SupportCaseCreateRequest(
                                safeCategory,
                                clean(orderId).isEmpty()
                                        ? null
                                        : clean(orderId)
                        )
                )
                .enqueue(
                        supportCaseResponseCallback(
                                callback,
                                true
                        )
                );
    }

    public void sendSupportMessage(
            String caseId,
            String message,
            long expectedRevision,
            String idempotencyKey,
            SupportCaseCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String safeCaseId =
                clean(caseId);

        String safeMessage =
                clean(message);

        String safeKey =
                clean(idempotencyKey);

        if (
                safeCaseId.isEmpty()
                        ||
                        safeMessage.isEmpty()
                        ||
                        expectedRevision < 1
                        ||
                        safeKey.isEmpty()
        ) {

            callback.onError(
                    0,
                    "Некорректное сообщение"
            );

            return;
        }

        api
                .sendSupportMessage(
                        safeCaseId,
                        safeKey,
                        new SupportMessageRequest(
                                safeMessage,
                                expectedRevision
                        )
                )
                .enqueue(
                        new Callback<SupportMessageMutationResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<SupportMessageMutationResponse> call,
                                    @NonNull Response<SupportMessageMutationResponse> response
                            ) {

                                if (
                                        handleSupportCaseAuth(
                                                response,
                                                callback
                                        )
                                ) {
                                    return;
                                }

                                if (
                                        response.code()
                                                == 409
                                ) {

                                    callback.onConflict();

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapWriteError(
                                                    response.code(),
                                                    "Не удалось отправить сообщение"
                                            )
                                    );

                                    return;
                                }

                                SupportMessageMutationResponse body =
                                        response.body();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                                ||
                                                body.getSupportCaseRevision()
                                                        == null
                                                ||
                                                body.getSupportCaseRevision()
                                                        < 1
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                getSupportCase(
                                        safeCaseId,
                                        callback
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<SupportMessageMutationResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось отправить сообщение. Повторите отправку"
                                );
                            }
                        }
                );
    }

    public void uploadPhotoCheckImage(
            DriverPhotoCheckDto check,
            String kind,
            File file,
            UploadCallback callback
    ) {

        if (callback == null) {
            return;
        }

        if (
                check == null
                        ||
                        clean(check.getId())
                                .isEmpty()
                        ||
                        clean(check.getVehicleId())
                                .isEmpty()
                        ||
                        clean(kind).isEmpty()
                        ||
                        file == null
                        ||
                        !file.isFile()
                        ||
                        file.length() <= 0
        ) {

            callback.onError(
                    0,
                    "Некорректный файл фотоконтроля"
            );

            return;
        }

        long contentLength =
                file.length();

        String contentType =
                "image/jpeg";

        api
                .issuePrivateUpload(
                        new DriverUploadIssueRequest(
                                "vehicle_photo",
                                clean(kind),
                                clean(check.getVehicleId()),
                                clean(check.getId()),
                                contentType,
                                contentLength
                        )
                )
                .enqueue(
                        new Callback<DriverUploadIssueResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<DriverUploadIssueResponse> call,
                                    @NonNull Response<DriverUploadIssueResponse> response
                            ) {

                                if (
                                        handleUploadAuth(
                                                response,
                                                callback
                                        )
                                ) {
                                    return;
                                }

                                if (
                                        response.code()
                                                == 409
                                ) {

                                    callback.onConflict();

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapWriteError(
                                                    response.code(),
                                                    "Не удалось подготовить загрузку"
                                            )
                                    );

                                    return;
                                }

                                DriverUploadIssueResponse body =
                                        response.body();

                                DriverUploadSessionDto upload =
                                        body == null
                                                ? null
                                                : body.getUpload();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                                ||
                                                upload == null
                                                ||
                                                clean(upload.getPath())
                                                        .isEmpty()
                                                ||
                                                !"PUT".equals(
                                                        clean(
                                                                upload.getMethod()
                                                        )
                                                )
                                                ||
                                                upload.getContentLength()
                                                        == null
                                                ||
                                                upload.getContentLength()
                                                        != contentLength
                                                ||
                                                !contentType.equals(
                                                        clean(
                                                                upload.getContentType()
                                                        )
                                                )
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                putPhoto(
                                        upload,
                                        file,
                                        callback
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<DriverUploadIssueResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось подготовить загрузку. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    public static String newMutationKey(
            String prefix
    ) {

        String safePrefix =
                clean(prefix);

        if (safePrefix.isEmpty()) {

            safePrefix =
                    "driver";
        }

        return safePrefix
                +
                "-"
                +
                UUID.randomUUID();
    }

    private void putPhoto(
            DriverUploadSessionDto upload,
            File file,
            UploadCallback callback
    ) {

        MediaType mediaType =
                MediaType.parse(
                        upload.getContentType()
                );

        if (mediaType == null) {

            callback.onError(
                    0,
                    "Некорректный тип файла"
            );

            return;
        }

        RequestBody body =
                RequestBody.create(
                        mediaType,
                        file
                );

        api
                .uploadPrivateResource(
                        upload.getPath(),
                        upload.getContentType(),
                        body
                )
                .enqueue(
                        new Callback<PrivateUploadResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<PrivateUploadResponse> call,
                                    @NonNull Response<PrivateUploadResponse> response
                            ) {

                                if (
                                        handleUploadAuth(
                                                response,
                                                callback
                                        )
                                ) {
                                    return;
                                }

                                if (
                                        response.code()
                                                == 409
                                ) {

                                    callback.onConflict();

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapWriteError(
                                                    response.code(),
                                                    "Не удалось загрузить фото"
                                            )
                                    );

                                    return;
                                }

                                PrivateUploadResponse responseBody =
                                        response.body();

                                if (
                                        responseBody == null
                                                ||
                                                !responseBody.isOk()
                                                ||
                                                responseBody.getResource()
                                                        == null
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                callback.onSuccess(
                                        responseBody.getResource()
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<PrivateUploadResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось загрузить фото. Повторите попытку"
                                );
                            }
                        }
                );
    }

    private Callback<SupportCaseResponse> supportCaseResponseCallback(
            SupportCaseCallback callback,
            boolean creating
    ) {

        return new Callback<SupportCaseResponse>() {

            @Override
            public void onResponse(
                    @NonNull Call<SupportCaseResponse> call,
                    @NonNull Response<SupportCaseResponse> response
            ) {

                if (
                        handleSupportCaseAuth(
                                response,
                                callback
                        )
                ) {
                    return;
                }

                if (
                        response.code()
                                == 409
                ) {

                    callback.onConflict();

                    return;
                }

                if (
                        !response.isSuccessful()
                ) {

                    callback.onError(
                            response.code(),
                            mapWriteError(
                                    response.code(),
                                    creating
                                            ? "Не удалось создать обращение"
                                            : "Не удалось загрузить обращение"
                            )
                    );

                    return;
                }

                SupportCaseResponse body =
                        response.body();

                if (
                        body == null
                                ||
                                !body.isOk()
                                ||
                                body.getSupportCase()
                                        == null
                ) {

                    callback.onError(
                            response.code(),
                            "Некорректный ответ сервера"
                    );

                    return;
                }

                callback.onSuccess(
                        body.getSupportCase()
                );
            }

            @Override
            public void onFailure(
                    @NonNull Call<SupportCaseResponse> call,
                    @NonNull Throwable throwable
            ) {

                if (call.isCanceled()) {
                    return;
                }

                callback.onError(
                        0,
                        creating
                                ? "Не удалось создать обращение. Проверьте интернет"
                                : "Не удалось загрузить обращение. Проверьте интернет"
                );
            }
        };
    }

    private boolean handlePhotoAuth(
            Response<?> response,
            PhotoChecksCallback callback
    ) {

        if (
                response.code()
                        != 401
        ) {
            return false;
        }

        if (
                isTemporaryAuthRecoveryFailure(
                        response
                )
        ) {

            callback.onError(
                    0,
                    "Не удалось обновить сессию. Проверьте подключение"
            );

        }
        else {

            callback.onAuthenticationRequired();
        }

        return true;
    }

    private boolean handleSupportListAuth(
            Response<?> response,
            SupportCasesCallback callback
    ) {

        if (
                response.code()
                        != 401
        ) {
            return false;
        }

        if (
                isTemporaryAuthRecoveryFailure(
                        response
                )
        ) {

            callback.onError(
                    0,
                    "Не удалось обновить сессию. Проверьте подключение"
            );

        }
        else {

            callback.onAuthenticationRequired();
        }

        return true;
    }

    private boolean handleSupportCaseAuth(
            Response<?> response,
            SupportCaseCallback callback
    ) {

        if (
                response.code()
                        != 401
        ) {
            return false;
        }

        if (
                isTemporaryAuthRecoveryFailure(
                        response
                )
        ) {

            callback.onError(
                    0,
                    "Не удалось обновить сессию. Проверьте подключение"
            );

        }
        else {

            callback.onAuthenticationRequired();
        }

        return true;
    }

    private boolean handleUploadAuth(
            Response<?> response,
            UploadCallback callback
    ) {

        if (
                response.code()
                        != 401
        ) {
            return false;
        }

        if (
                isTemporaryAuthRecoveryFailure(
                        response
                )
        ) {

            callback.onError(
                    0,
                    "Не удалось обновить сессию. Проверьте подключение"
            );

        }
        else {

            callback.onAuthenticationRequired();
        }

        return true;
    }

    private boolean isTemporaryAuthRecoveryFailure(
            Response<?> response
    ) {

        String value =
                response
                        .headers()
                        .get(
                                StaffAuthInterceptor
                                        .AUTH_RECOVERY_HEADER
                        );

        return StaffAuthInterceptor
                .AUTH_RECOVERY_TEMPORARY
                .equals(
                        value
                );
    }

    private String mapReadError(
            int status,
            String fallback
    ) {

        if (status == 403) {
            return "Доступ запрещён";
        }

        if (status == 404) {
            return "Данные больше не доступны";
        }

        if (status >= 500) {
            return "Сервер временно недоступен";
        }

        return fallback;
    }

    private String mapWriteError(
            int status,
            String fallback
    ) {

        if (status == 400) {
            return "Проверьте введённые данные";
        }

        if (status == 403) {
            return "Действие запрещено";
        }

        if (status == 404) {
            return "Данные больше не доступны";
        }

        if (status == 409) {
            return "Данные изменились. Обновите экран";
        }

        if (status == 413) {
            return "Файл слишком большой";
        }

        if (status == 422) {
            return "Действие сейчас недоступно";
        }

        if (status >= 500) {
            return "Сервер временно недоступен";
        }

        return fallback;
    }

    private static String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
