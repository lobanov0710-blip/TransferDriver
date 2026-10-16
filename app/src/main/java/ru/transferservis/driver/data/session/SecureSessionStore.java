package ru.transferservis.driver.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;

public final class SecureSessionStore {

    private static final String ANDROID_KEYSTORE =
            "AndroidKeyStore";

    private static final String KEY_ALIAS =
            "transfer_driver_session_key_v1";

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final String PREFS_NAME =
            "driver_secure_session_v1";

    private static final String PREF_IV =
            "session_iv";

    private static final String PREF_CIPHERTEXT =
            "session_ciphertext";

    private static final byte[] AAD =
            "ru.transferservis.driver|driver-session|v1"
                    .getBytes(StandardCharsets.UTF_8);

    private final SharedPreferences preferences;

    public SecureSessionStore(
            Context context
    ) {

        preferences =
                context
                        .getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    public boolean save(
            StaffLoginResponse response
    ) {

        if (response == null) {
            return false;
        }

        StaffDto staff =
                response.getStaff();

        if (staff == null) {
            return false;
        }

        DriverSession session =
                new DriverSession(
                        staff.getId(),
                        staff.getDisplayName(),
                        staff.getRole(),
                        response.getAccessToken(),
                        response.getAccessExpiresAt(),
                        response.getRefreshToken(),
                        response.getRefreshExpiresAt()
                );

        return save(
                session
        );
    }

    public boolean save(
            DriverSession session
    ) {

        if (!isValidSession(
                session
        )) {

            return false;
        }

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "staffId",
                    session.getStaffId()
            );

            json.put(
                    "displayName",
                    session.getDisplayName()
            );

            json.put(
                    "role",
                    session.getRole()
            );

            json.put(
                    "accessToken",
                    session.getAccessToken()
            );

            json.put(
                    "accessExpiresAt",
                    session.getAccessExpiresAt()
            );

            json.put(
                    "refreshToken",
                    session.getRefreshToken()
            );

            json.put(
                    "refreshExpiresAt",
                    session.getRefreshExpiresAt()
            );

            SecretKey secretKey =
                    getOrCreateSecretKey();

            Cipher cipher =
                    Cipher.getInstance(
                            TRANSFORMATION
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey
            );

            cipher.updateAAD(
                    AAD
            );

            byte[] ciphertext =
                    cipher.doFinal(
                            json
                                    .toString()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            byte[] iv =
                    cipher.getIV();

            if (
                    iv == null
                            ||
                            iv.length == 0
                            ||
                            ciphertext.length == 0
            ) {

                return false;
            }

            String encodedIv =
                    Base64.encodeToString(
                            iv,
                            Base64.NO_WRAP
                    );

            String encodedCiphertext =
                    Base64.encodeToString(
                            ciphertext,
                            Base64.NO_WRAP
                    );

            return preferences
                    .edit()
                    .putString(
                            PREF_IV,
                            encodedIv
                    )
                    .putString(
                            PREF_CIPHERTEXT,
                            encodedCiphertext
                    )
                    .commit();

        }
        catch (Exception exception) {

            clear();

            return false;
        }
    }

    public DriverSession load() {

        String encodedIv =
                preferences.getString(
                        PREF_IV,
                        null
                );

        String encodedCiphertext =
                preferences.getString(
                        PREF_CIPHERTEXT,
                        null
                );

        if (
                encodedIv == null
                        ||
                        encodedCiphertext == null
        ) {

            return null;
        }

        try {

            byte[] iv =
                    Base64.decode(
                            encodedIv,
                            Base64.NO_WRAP
                    );

            byte[] ciphertext =
                    Base64.decode(
                            encodedCiphertext,
                            Base64.NO_WRAP
                    );

            SecretKey secretKey =
                    getExistingSecretKey();

            if (secretKey == null) {

                clear();

                return null;
            }

            Cipher cipher =
                    Cipher.getInstance(
                            TRANSFORMATION
                    );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            128,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            cipher.updateAAD(
                    AAD
            );

            byte[] plaintext =
                    cipher.doFinal(
                            ciphertext
                    );

            JSONObject json =
                    new JSONObject(
                            new String(
                                    plaintext,
                                    StandardCharsets.UTF_8
                            )
                    );

            DriverSession session =
                    new DriverSession(
                            requireString(
                                    json,
                                    "staffId"
                            ),
                            requireString(
                                    json,
                                    "displayName"
                            ),
                            requireString(
                                    json,
                                    "role"
                            ),
                            requireString(
                                    json,
                                    "accessToken"
                            ),
                            json.getLong(
                                    "accessExpiresAt"
                            ),
                            requireString(
                                    json,
                                    "refreshToken"
                            ),
                            json.getLong(
                                    "refreshExpiresAt"
                            )
                    );

            if (!isValidSession(
                    session
            )) {

                clear();

                return null;
            }

            return session;

        }
        catch (Exception exception) {

            clear();

            return null;
        }
    }

    public boolean hasRefreshableDriverSession() {

        DriverSession session =
                load();

        return session != null
                && session.isDriver()
                && session.hasUsableRefreshToken();
    }

    public void clear() {

        preferences
                .edit()
                .clear()
                .apply();
    }

    private boolean isValidSession(
            DriverSession session
    ) {

        if (session == null) {
            return false;
        }

        if (!session.isDriver()) {
            return false;
        }

        if (isBlank(
                session.getStaffId()
        )) {

            return false;
        }

        if (isBlank(
                session.getAccessToken()
        )) {

            return false;
        }

        if (isBlank(
                session.getRefreshToken()
        )) {

            return false;
        }

        if (
                session.getAccessExpiresAt()
                        <= 0
        ) {

            return false;
        }

        return session
                .getRefreshExpiresAt()
                > 0;
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String requireString(
            JSONObject json,
            String name
    ) throws JSONException {

        String value =
                json.getString(
                        name
                );

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            throw new JSONException(
                    "Invalid session value"
            );
        }

        return value;
    }

    private SecretKey getExistingSecretKey()
            throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance(
                        ANDROID_KEYSTORE
                );

        keyStore.load(
                null
        );

        KeyStore.Entry entry =
                keyStore.getEntry(
                        KEY_ALIAS,
                        null
                );

        if (
                !(entry instanceof
                        KeyStore.SecretKeyEntry)
        ) {

            return null;
        }

        return ((KeyStore.SecretKeyEntry) entry)
                .getSecretKey();
    }

    private SecretKey getOrCreateSecretKey()
            throws Exception {

        SecretKey existing =
                getExistingSecretKey();

        if (existing != null) {
            return existing;
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE
                );

        KeyGenParameterSpec spec =
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                |
                                KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setKeySize(
                                256
                        )
                        .setRandomizedEncryptionRequired(
                                true
                        )
                        .build();

        keyGenerator.init(
                spec
        );

        return keyGenerator
                .generateKey();
    }
}