package ru.transferservis.driver.contract;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Url;

import ru.transferservis.driver.data.api.DriverV1Api;
import ru.transferservis.driver.data.model.SupportMessageRequest;

import static org.junit.Assert.*;

public final class DriverV1ApiContractTest {

    @Test
    public void photoControlReadUsesOwnDriverEndpoint()
            throws Exception {

        Method method =
                DriverV1Api.class
                        .getMethod(
                                "getPhotoChecks"
                        );

        GET get =
                method.getAnnotation(
                        GET.class
                );

        assertNotNull(
                get
        );

        assertEquals(
                "v1/driver/photo-checks",
                get.value()
        );
    }


    @Test
    public void supportCaseListAndDetailUseDriverScopedEndpoints()
            throws Exception {

        GET list =
                DriverV1Api.class
                        .getMethod(
                                "getSupportCases",
                                int.class
                        )
                        .getAnnotation(
                                GET.class
                        );

        GET detail =
                DriverV1Api.class
                        .getMethod(
                                "getSupportCase",
                                String.class
                        )
                        .getAnnotation(
                                GET.class
                        );

        assertNotNull(
                list
        );

        assertNotNull(
                detail
        );

        assertEquals(
                "v1/driver/support-cases",
                list.value()
        );

        assertEquals(
                "v1/driver/support-cases/{caseId}",
                detail.value()
        );
    }


    @Test
    public void supportMessageCarriesIdempotencyAndExpectedRevision()
            throws Exception {

        Method method =
                DriverV1Api.class
                        .getMethod(
                                "sendSupportMessage",
                                String.class,
                                String.class,
                                SupportMessageRequest.class
                        );

        POST post =
                method.getAnnotation(
                        POST.class
                );

        assertNotNull(
                post
        );

        assertEquals(
                "v1/driver/support-cases/{caseId}/messages",
                post.value()
        );


        Annotation[] headerAnnotations =
                method
                        .getParameterAnnotations()[1];

        boolean hasIdempotency =
                Arrays.stream(
                                headerAnnotations
                        )
                        .filter(
                                annotation ->
                                        annotation instanceof Header
                        )
                        .map(
                                annotation ->
                                        (Header) annotation
                        )
                        .anyMatch(
                                header ->
                                        "Idempotency-Key".equals(
                                                header.value()
                                        )
                        );

        assertTrue(
                hasIdempotency
        );


        SupportMessageRequest request =
                new SupportMessageRequest(
                        "Тест",
                        7L
                );

        assertEquals(
                "Тест",
                request.getBody()
        );

        assertEquals(
                7L,
                request.getExpectedRevision()
        );
    }


    @Test
    public void privatePhotoUploadUsesIssueThenRelativePut()
            throws Exception {

        POST issue =
                DriverV1Api.class
                        .getMethod(
                                "issuePrivateUpload",
                                ru.transferservis.driver.data.model.DriverUploadIssueRequest.class
                        )
                        .getAnnotation(
                                POST.class
                        );

        assertNotNull(
                issue
        );

        assertEquals(
                "v1/driver/uploads",
                issue.value()
        );


        Method putMethod =
                DriverV1Api.class
                        .getMethod(
                                "uploadPrivateResource",
                                String.class,
                                String.class,
                                okhttp3.RequestBody.class
                        );

        PUT put =
                putMethod.getAnnotation(
                        PUT.class
                );

        assertNotNull(
                put
        );

        assertEquals(
                "",
                put.value()
        );


        boolean relativeUrl =
                Arrays.stream(
                                putMethod
                                        .getParameterAnnotations()[0]
                        )
                        .anyMatch(
                                annotation ->
                                        annotation instanceof Url
                        );

        assertTrue(
                relativeUrl
        );


        boolean contentTypeHeader =
                Arrays.stream(
                                putMethod
                                        .getParameterAnnotations()[1]
                        )
                        .filter(
                                annotation ->
                                        annotation instanceof Header
                        )
                        .map(
                                annotation ->
                                        (Header) annotation
                        )
                        .anyMatch(
                                header ->
                                        "Content-Type".equals(
                                                header.value()
                                        )
                        );

        assertTrue(
                contentTypeHeader
        );
    }
}
