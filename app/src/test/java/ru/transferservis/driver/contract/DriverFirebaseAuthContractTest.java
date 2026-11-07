package ru.transferservis.driver.contract;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.http.Header;
import retrofit2.http.POST;

import ru.transferservis.driver.data.api.StaffApi;
import ru.transferservis.driver.data.model.DriverFirebaseSessionExchangeRequest;

import static org.junit.Assert.*;

public final class DriverFirebaseAuthContractTest {

    @Test
    public void sessionExchangeUsesApprovedD2EndpointAndAppCheckHeader()
            throws Exception {

        Method method =
                StaffApi.class
                        .getMethod(
                                "exchangeDriverFirebaseSession",
                                String.class,
                                DriverFirebaseSessionExchangeRequest.class
                        );

        POST post =
                method.getAnnotation(
                        POST.class
                );

        assertNotNull(
                post
        );

        assertEquals(
                "v1/driver/session/exchange",
                post.value()
        );

        Annotation[] headers =
                method
                        .getParameterAnnotations()[0];

        boolean hasAppCheck =
                Arrays.stream(
                                headers
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
                                        "X-Firebase-AppCheck".equals(
                                                header.value()
                                        )
                        );

        assertTrue(
                hasAppCheck
        );
    }

    @Test
    public void exchangeBodyContainsOnlyFirebaseTokenAndInstallationId() {

        Set<String> fields =
                Arrays.stream(
                                DriverFirebaseSessionExchangeRequest.class
                                        .getDeclaredFields()
                        )
                        .map(
                                Field::getName
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                Set.of(
                        "firebaseIdToken",
                        "installationId"
                ),
                fields
        );

        assertFalse(
                fields.contains(
                        "phone"
                )
        );

        assertFalse(
                fields.contains(
                        "role"
                )
        );

        assertFalse(
                fields.contains(
                        "driverId"
                )
        );
    }
}
