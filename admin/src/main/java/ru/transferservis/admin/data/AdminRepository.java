package ru.transferservis.admin.data;

import android.content.Context;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AdminRepository {
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object stateLock = new Object();
    private long generation;
    private final AdminSessionStore store;
    private final AdminApiClient api;
    public AdminRepository(Context context) { this(context, new AdminApiClient()); }
    AdminRepository(Context context, AdminApiClient api) { store = new AdminSessionStore(context); this.api = api; }
    public boolean hasSession() { return store.load() != null; }
    public long generation() { synchronized (stateLock) { return generation; } }
    public AdminAccess login(String login, String password, long expectedGeneration) throws IOException {
        JsonObject response = api.login(login, password);
        if (response.has("mfaRequired")) {
            final OwnerMfa.Challenge challenge;
            try { challenge = OwnerMfa.Challenge.parse(response); }
            catch (IOException invalid) { revokeToken(AdminSession.string(response, "refreshToken")); throw invalid; }
            final AdminSession previous;
            synchronized (stateLock) {
                if (generation != expectedGeneration) throw new IOException("Session replaced");
                previous = store.load(); store.clear();
            }
            if (previous != null) revoke(previous);
            throw new OwnerMfa.Required(challenge);
        }
        return finishLogin(response, expectedGeneration);
    }
    public AdminAccess verifyMfa(OwnerMfa.Challenge challenge, String code, boolean recovery, long expectedGeneration) throws IOException {
        if (challenge == null || !challenge.active() || challenge.enrollmentRequired) throw new IOException("MFA challenge expired");
        return finishLogin(api.verifyMfa(challenge.id, code, recovery), expectedGeneration);
    }
    public OwnerMfa.Enrollment startEnrollment(OwnerMfa.Challenge challenge, String bootstrap) throws IOException {
        if (challenge == null || !challenge.active() || !challenge.enrollmentRequired) throw new IOException("MFA challenge expired");
        return OwnerMfa.Enrollment.parse(api.startEnrollment(challenge.id, bootstrap));
    }
    public java.util.List<String> confirmEnrollment(OwnerMfa.Enrollment enrollment, String code) throws IOException {
        if (enrollment == null || !enrollment.active()) throw new IOException("MFA enrollment expired");
        return OwnerMfa.recoveryCodes(api.confirmEnrollment(enrollment.id, code));
    }
    private AdminAccess finishLogin(JsonObject response, long expectedGeneration) throws IOException {
        AdminSession candidate = AdminSession.fromResponse(response);
        if (candidate == null || !candidate.accessUsable() || !candidate.refreshUsable()) {
            revokeToken(AdminSession.string(response, "refreshToken"));
            throw new IOException("Invalid session");
        }
        try {
            if (generation() != expectedGeneration) throw new IOException("Session replaced");
            AdminAccess access = AdminAccess.fromResponse(api.me(candidate.accessToken), candidate.accountId);
            if (access == null || (access.isOwner() && !access.hasVerifiedMfa())) throw new IOException("Admin access required");
            synchronized (stateLock) {
                if (generation != expectedGeneration || !store.save(candidate)) throw new IOException("Session replaced");
            }
            return access;
        } catch (IOException failure) { revoke(candidate); throw failure; }
    }
    public AdminAccess me() throws IOException {
        AdminSession current = requireSession();
        try {
            return readAccess(current);
        } catch (AdminApiClient.Failure failure) {
            if (failure.status != 401) throw failure;
            AdminSession rotated = refresh(current);
            try { return readAccess(rotated); }
            catch (AdminApiClient.Failure denied) {
                if (denied.status == 401) store.clearIfCurrent(rotated);
                throw denied;
            }
        }
    }
    private AdminAccess readAccess(AdminSession session) throws IOException {
        JsonObject response;
        try { response = api.me(session.accessToken); }
        catch (AdminApiClient.Failure failure) {
            if (failure.status == 403) store.clearIfCurrent(session);
            throw failure;
        }
        AdminAccess access = AdminAccess.fromResponse(response, session.accountId);
        if (access == null || (access.isOwner() && !access.hasVerifiedMfa())) { store.clearIfCurrent(session); throw new IOException("Admin access required"); }
        return access;
    }
    public static final class QueueResult {
        public final AdminAccess access;
        public final JsonObject response;
        QueueResult(AdminAccess access, JsonObject response) { this.access = access; this.response = response; }
    }
    public QueueResult registrations() throws IOException {
        // Resolve current permissions on the server for every queue read; no persisted grants.
        AdminAccess access = me();
        if (!access.canReadRegistrations()) throw new IOException("Permission required");
        AdminSession current = requireSession();
        if (!current.accountId.equals(access.accountId)) throw new IOException("Session replaced");
        try { return new QueueResult(access, api.registrations(current.accessToken)); }
        catch (AdminApiClient.Failure failure) {
            if (failure.status != 401) throw failure;
            // A rotated session can lose permission; re-resolve before retrying this read-only GET.
            AdminSession rotated = refresh(current);
            AdminAccess updated = readAccess(rotated);
            if (!updated.canReadRegistrations()) throw new IOException("Permission required");
            return new QueueResult(updated, api.registrations(rotated.accessToken));
        }
    }
    private AdminSession requireSession() throws IOException {
        AdminSession current = store.load();
        if (current == null) throw new AdminApiClient.Failure(401);
        return current.accessUsable() ? current : refresh(current);
    }
    private AdminSession refresh(AdminSession expected) throws IOException {
        if (!expected.same(store.load())) throw new IOException("Session replaced");
        try {
            AdminSession replacement = AdminSession.fromResponse(api.refresh(expected.refreshToken));
            AdminSession rotated = store.rotateIfCurrent(expected, replacement);
            if (rotated == null) throw new AdminApiClient.Failure(401);
            return rotated;
        } catch (AdminApiClient.Failure failure) {
            if (failure.status == 401 || failure.status == 403) store.clearIfCurrent(expected);
            throw failure;
        }
    }
    public void logout() {
        final AdminSession previous;
        synchronized (stateLock) { generation++; previous = store.load(); store.clear(); }
        if (previous != null) executor.execute(() -> revoke(previous));
    }
    private void revoke(AdminSession session) { revokeToken(session.refreshToken); }
    private void revokeToken(String token) {
        if (token.isEmpty() || token.length() > 1024 || !token.matches("[A-Za-z0-9._~-]+")) return;
        try { api.logout(token); } catch (IOException ignored) { /* Rejected/local session cannot be used by this client. */ }
    }
}
