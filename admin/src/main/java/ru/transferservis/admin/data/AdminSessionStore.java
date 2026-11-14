package ru.transferservis.admin.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import ru.transferservis.admin.security.AdminEndpointPolicy;

public final class AdminSessionStore {
    private static final Object LOCK = new Object();
    private static final String ALIAS = "transfer_admin_session_key_v1";
    private static final byte[] AAD = ("ru.transferservis.admin|staff-session|v1|" + AdminEndpointPolicy.BASE_URL)
            .getBytes(StandardCharsets.UTF_8);
    private final SharedPreferences prefs;
    public AdminSessionStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("admin_secure_session_v1", Context.MODE_PRIVATE);
    }
    public boolean save(AdminSession session) { synchronized (LOCK) { return saveLocked(session); } }
    private boolean saveLocked(AdminSession session) {
        if (session == null || !session.valid() || !session.refreshUsable()) return false;
        try {
            SecretKey key = existingKey();
            if (key == null) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256).setRandomizedEncryptionRequired(true).build());
                key = generator.generateKey();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key); cipher.updateAAD(AAD);
            byte[] ciphertext = cipher.doFinal(session.toResponse().toString().getBytes(StandardCharsets.UTF_8));
            boolean saved = prefs.edit().putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP)).commit();
            if (!saved) clearLocked();
            return saved;
        } catch (Exception failure) { clearLocked(); return false; }
    }
    public AdminSession load() { synchronized (LOCK) { return loadLocked(); } }
    private AdminSession loadLocked() {
        try {
            String iv = prefs.getString("iv", null), ciphertext = prefs.getString("ciphertext", null);
            if (iv == null && ciphertext == null) return null;
            if (iv == null || ciphertext == null || ciphertext.length() > 100_000) { clearLocked(); return null; }
            SecretKey key = existingKey();
            if (key == null) { clearLocked(); return null; }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            cipher.updateAAD(AAD);
            String json = new String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8);
            AdminSession session = AdminSession.fromResponse(JsonParser.parseString(json).getAsJsonObject());
            if (session == null || !session.refreshUsable()) { clearLocked(); return null; }
            return session;
        } catch (Exception failure) { clearLocked(); return null; }
    }
    public AdminSession rotateIfCurrent(AdminSession expected, AdminSession replacement) {
        synchronized (LOCK) {
            if (expected == null || !expected.same(loadLocked())) return null;
            if (replacement == null || !expected.accountId.equals(replacement.accountId)
                    || expected.refreshToken.equals(replacement.refreshToken)
                    || !replacement.accessUsable() || !replacement.refreshUsable() || !saveLocked(replacement)) {
                clearLocked(); return null;
            }
            return replacement;
        }
    }
    public void clearIfCurrent(AdminSession expected) {
        synchronized (LOCK) { if (expected != null && expected.same(loadLocked())) clearLocked(); }
    }
    public void clear() { synchronized (LOCK) { clearLocked(); } }
    @SuppressLint("ApplySharedPref") // Logout must finish the durable clear before returning.
    private void clearLocked() {
        prefs.edit().clear().commit();
        // Also invalidate ciphertext on disk if preference persistence fails.
        try { KeyStore keys = KeyStore.getInstance("AndroidKeyStore"); keys.load(null); keys.deleteEntry(ALIAS); }
        catch (Exception unavailable) { /* No session is retained in app memory. */ }
    }
    private SecretKey existingKey() throws Exception {
        KeyStore keys = KeyStore.getInstance("AndroidKeyStore"); keys.load(null);
        KeyStore.Entry entry = keys.getEntry(ALIAS, null);
        return entry instanceof KeyStore.SecretKeyEntry ? ((KeyStore.SecretKeyEntry) entry).getSecretKey() : null;
    }
}
