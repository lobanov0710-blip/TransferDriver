package ru.transferservis.driver.data.repository;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.DriverV1Api;
import ru.transferservis.driver.data.model.SupportCaseResponse;
import ru.transferservis.driver.data.model.SupportMessageMutationResponse;
import ru.transferservis.driver.data.model.SupportMessageRequest;

import static org.junit.Assert.*;

public final class DriverV1RepositorySupportContractTest {

    private static final class Outcome implements DriverV1Repository.SupportCaseCallback {
        int conflict;
        int auth;
        int success;
        int errors;
        int status;
        @Override public void onSuccess(ru.transferservis.driver.data.model.SupportCaseDto item) {
            success++;
        }
        @Override public void onAuthenticationRequired() { auth++; }
        @Override public void onConflict() { conflict++; }
        @Override public void onError(int code, String message) {
            errors++;
            status = code;
        }
    }

    private static final class Backend {
        final AtomicInteger requests = new AtomicInteger();
        String caseId;
        String key;
        SupportMessageRequest request;
        int code = 409;
        boolean offline;

        DriverV1Api api() {
            return (DriverV1Api) Proxy.newProxyInstance(
                    DriverV1Api.class.getClassLoader(),
                    new Class<?>[]{DriverV1Api.class},
                    (proxy, method, args) -> {
                        if ("sendSupportMessage".equals(method.getName())) {
                            requests.incrementAndGet();
                            caseId = (String) args[0];
                            key = (String) args[1];
                            request = (SupportMessageRequest) args[2];
                            return call();
                        }
                        throw new AssertionError("Unexpected API request: " + method.getName());
                    });
        }

        @SuppressWarnings("unchecked")
        Call<SupportMessageMutationResponse> call() {
            return (Call<SupportMessageMutationResponse>) Proxy.newProxyInstance(
                    Call.class.getClassLoader(), new Class<?>[]{Call.class},
                    (proxy, method, args) -> {
                        if ("enqueue".equals(method.getName())) {
                            Callback<SupportMessageMutationResponse> cb =
                                    (Callback<SupportMessageMutationResponse>) args[0];
                            if (offline) {
                                cb.onFailure((Call<SupportMessageMutationResponse>) proxy,
                                        new IOException("Synthetic offline"));
                            } else {
                                cb.onResponse((Call<SupportMessageMutationResponse>) proxy,
                                        Response.error(code,
                                                ResponseBody.create(okhttp3.MediaType.parse("application/json"),
                                                        "{}")));
                            }
                            return null;
                        }
                        if ("isCanceled".equals(method.getName())) return false;
                        if ("clone".equals(method.getName())) return call();
                        throw new AssertionError("Unexpected Call operation: " + method.getName());
                    });
        }
    }

    @Test
    public void invalidMutationsNeverReachNetwork() {
        Backend backend = new Backend();
        DriverV1Repository repository = new DriverV1Repository(backend.api());
        Outcome outcome = new Outcome();
        repository.sendSupportMessage("", "message", 4, "key", outcome);
        repository.sendSupportMessage("case-1", "  ", 4, "key", outcome);
        repository.sendSupportMessage("case-1", "message", 0, "key", outcome);
        repository.sendSupportMessage("case-1", "message", 4, " ", outcome);
        assertEquals(0, backend.requests.get());
        assertEquals(4, outcome.errors);
        assertEquals(0, outcome.status);
    }

    @Test
    public void conflictIsNotSilentlyRetriedOrReportedAsSuccess() {
        Backend backend = new Backend();
        DriverV1Repository repository = new DriverV1Repository(backend.api());
        Outcome outcome = new Outcome();
        repository.sendSupportMessage("case-1", "Hello", 7, "key-7", outcome);
        assertEquals(1, backend.requests.get());
        assertEquals("case-1", backend.caseId);
        assertEquals("key-7", backend.key);
        assertEquals("Hello", backend.request.getBody());
        assertEquals(7, backend.request.getExpectedRevision());
        assertEquals(1, outcome.conflict);
        assertEquals(0, outcome.success);
        assertEquals(0, outcome.errors);
    }

    @Test
    public void offlineFailureCanBeRetriedWithSameKeyAndRevision() {
        Backend backend = new Backend();
        backend.offline = true;
        DriverV1Repository repository = new DriverV1Repository(backend.api());
        Outcome first = new Outcome();
        repository.sendSupportMessage("case-1", "Hello", 7, "key-7", first);
        assertEquals(1, first.errors);
        assertEquals(0, first.status);
        assertEquals(1, backend.requests.get());

        Outcome second = new Outcome();
        repository.sendSupportMessage("case-1", "Hello", 7, "key-7", second);
        assertEquals(2, backend.requests.get());
        assertEquals("key-7", backend.key);
        assertEquals(7, backend.request.getExpectedRevision());
        assertEquals(1, second.errors);
        assertEquals(0, second.success);
    }
}
