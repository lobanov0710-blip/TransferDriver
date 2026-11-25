package ru.transferservis.driver.contract;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import ru.transferservis.driver.data.api.OrderApi;
import ru.transferservis.driver.data.model.OrderStatusRequest;
import ru.transferservis.driver.data.model.StaffOrderDto;

import static org.junit.Assert.*;

public final class DriverOrderApiContractTest {

    @Test
    public void assignedOnlyReadUsesV1DriverAssignments() throws Exception {

        Method method =
                OrderApi.class
                        .getMethod(
                                "getAssignments",
                                int.class
                        );

        GET get =
                method.getAnnotation(
                        GET.class
                );

        assertNotNull(
                get
        );

        assertEquals(
                "v1/driver/assignments",
                get.value()
        );


        boolean legacyGetOrders =
                Arrays.stream(
                                OrderApi.class
                                        .getMethods()
                        )
                        .anyMatch(
                                candidate ->
                                        "getOrders".equals(
                                                candidate.getName()
                                        )
                        );

        assertFalse(
                "Legacy GET /orders must not return to Driver API",
                legacyGetOrders
        );
    }


    @Test
    public void terminalDoneContractCarriesIdempotencyHeaderAndRevision()
            throws Exception {

        Method method =
                OrderApi.class
                        .getMethod(
                                "changeOrderStatus",
                                String.class,
                                String.class,
                                OrderStatusRequest.class
                        );

        POST post =
                method.getAnnotation(
                        POST.class
                );

        assertNotNull(
                post
        );

        assertEquals(
                "orders/{orderId}/status",
                post.value()
        );


        Annotation[] idempotencyAnnotations =
                method
                        .getParameterAnnotations()[1];

        boolean hasIdempotencyHeader =
                Arrays.stream(
                                idempotencyAnnotations
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
                hasIdempotencyHeader
        );


        OrderStatusRequest request =
                new OrderStatusRequest(
                        "done",
                        17L
                );

        assertEquals(
                "done",
                request.getStatus()
        );

        assertEquals(
                17L,
                request.getExpectedRevision()
        );
    }


    @Test
    public void assignedOrderCarriesServerRevisionsAndPhaseContext() {

        StaffOrderDto order =
                new StaffOrderDto();

        order.bindAssignmentContext(
                "assignment_12345678",
                5L,
                "in_trip",
                1800000000100L,
                1800000000000L,
                "vehicle_12345678"
        );

        assertEquals(
                "assignment_12345678",
                order.getAssignmentId()
        );

        assertEquals(
                Long.valueOf(5L),
                order.getAssignmentRevision()
        );

        assertEquals(
                "in_trip",
                order.getAssignmentPhase()
        );

        assertEquals(
                Long.valueOf(1800000000100L),
                order.getPhaseUpdatedAt()
        );

        assertEquals(
                Long.valueOf(1800000000000L),
                order.getAssignedAt()
        );

        assertEquals(
                "vehicle_12345678",
                order.getVehicleId()
        );
    }
}
