package ru.transferservis.driver.ui.photo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;
import ru.transferservis.driver.data.model.PrivateResourceDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.security.SecureActivity;
import ru.transferservis.driver.ui.login.LoginActivity;

public final class PhotoCheckActivity
        extends SecureActivity {

    private static final String EXTRA_CHECK_ID =
            "photo_check_id";

    private static final String STATE_CAPTURE_KIND =
            "photo_capture_kind";

    private static final String STATE_CAPTURE_PATH =
            "photo_capture_path";

    private static final String STATE_PENDING_KIND =
            "photo_pending_kind";

    private static final String STATE_PENDING_PATH =
            "photo_pending_path";

    private static final String[] KINDS = {
            "left",
            "front",
            "right",
            "rear",
            "open_trunk",
            "rear_cabin",
            "front_cabin"
    };

    private final DriverV1Repository repository =
            new DriverV1Repository();

    private final Map<String, Button> buttons =
            new HashMap<>();

    private TextView status;
    private TextView reason;
    private TextView progressText;
    private ProgressBar progress;
    private Button refresh;

    private ActivityResultLauncher<Uri> takePicture;

    private String checkId;
    private DriverPhotoCheckDto currentCheck;

    private String captureKind;
    private File captureFile;

    private String pendingUploadKind;
    private File pendingUploadFile;

    private boolean uploading;

    public static Intent createIntent(
            Context context,
            String checkId
    ) {

        Intent intent =
                new Intent(
                        context,
                        PhotoCheckActivity.class
                );

        intent.putExtra(
                EXTRA_CHECK_ID,
                checkId
        );

        return intent;
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_photo_check
        );

        checkId =
                getIntent()
                        .getStringExtra(
                                EXTRA_CHECK_ID
                        );

        if (
                checkId == null
                        ||
                        checkId.trim().isEmpty()
        ) {

            finish();
            return;
        }

        checkId =
                checkId.trim();

        status =
                findViewById(
                        R.id.textPhotoCheckDetailStatus
                );

        reason =
                findViewById(
                        R.id.textPhotoCheckDetailReason
                );

        progressText =
                findViewById(
                        R.id.textPhotoCheckDetailProgress
                );

        progress =
                findViewById(
                        R.id.progressPhotoUpload
                );

        refresh =
                findViewById(
                        R.id.buttonPhotoCheckRefresh
                );

        buttons.put(
                "left",
                findViewById(
                        R.id.buttonPhotoLeft
                )
        );

        buttons.put(
                "front",
                findViewById(
                        R.id.buttonPhotoFront
                )
        );

        buttons.put(
                "right",
                findViewById(
                        R.id.buttonPhotoRight
                )
        );

        buttons.put(
                "rear",
                findViewById(
                        R.id.buttonPhotoRear
                )
        );

        buttons.put(
                "open_trunk",
                findViewById(
                        R.id.buttonPhotoTrunk
                )
        );

        buttons.put(
                "rear_cabin",
                findViewById(
                        R.id.buttonPhotoRearCabin
                )
        );

        buttons.put(
                "front_cabin",
                findViewById(
                        R.id.buttonPhotoFrontCabin
                )
        );

        for (String kind : KINDS) {

            Button button =
                    buttons.get(
                            kind
                    );

            if (button != null) {

                button.setOnClickListener(
                        view ->
                                onPhotoAction(
                                        kind
                                )
                );
            }
        }

        refresh.setOnClickListener(
                view ->
                        loadCheck()
        );

        if (savedInstanceState != null) {

            captureKind =
                    savedInstanceState
                            .getString(
                                    STATE_CAPTURE_KIND
                            );

            String capturePath =
                    savedInstanceState
                            .getString(
                                    STATE_CAPTURE_PATH
                            );

            if (
                    capturePath != null
                            &&
                            !capturePath.trim().isEmpty()
            ) {

                captureFile =
                        new File(
                                capturePath
                        );
            }

            pendingUploadKind =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_KIND
                            );

            String pendingPath =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_PATH
                            );

            if (
                    pendingPath != null
                            &&
                            !pendingPath.trim().isEmpty()
            ) {

                pendingUploadFile =
                        new File(
                                pendingPath
                        );
            }
        }

        takePicture =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        success ->
                                onCameraResult(
                                        Boolean.TRUE.equals(
                                                success
                                        )
                                )
                );

        cleanupOldPhotoCache();

        loadCheck();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {

        outState.putString(
                STATE_CAPTURE_KIND,
                captureKind
        );

        outState.putString(
                STATE_CAPTURE_PATH,
                captureFile == null
                        ? null
                        : captureFile.getAbsolutePath()
        );

        outState.putString(
                STATE_PENDING_KIND,
                pendingUploadKind
        );

        outState.putString(
                STATE_PENDING_PATH,
                pendingUploadFile == null
                        ? null
                        : pendingUploadFile.getAbsolutePath()
        );

        super.onSaveInstanceState(
                outState
        );
    }

    @Override
    protected void onDestroy() {

        if (
                isFinishing()
                        &&
                        !isChangingConfigurations()
        ) {

            deleteQuietly(
                    captureFile
            );

            deleteQuietly(
                    pendingUploadFile
            );
        }

        super.onDestroy();
    }

    private void loadCheck() {

        if (uploading) {
            return;
        }

        progress.setVisibility(
                View.VISIBLE
        );

        status.setText(
                R.string.photo_control_loading
        );

        repository.getPhotoChecks(
                new DriverV1Repository.PhotoChecksCallback() {

                    @Override
                    public void onSuccess(
                            List<DriverPhotoCheckDto> checks
                    ) {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    DriverPhotoCheckDto found =
                                            null;

                                    if (checks != null) {

                                        for (
                                                DriverPhotoCheckDto check
                                                :
                                                checks
                                        ) {

                                            if (
                                                    check != null
                                                            &&
                                                            checkId.equals(
                                                                    check.getId()
                                                            )
                                            ) {

                                                found =
                                                        check;

                                                break;
                                            }
                                        }
                                    }

                                    if (found == null) {

                                        currentCheck =
                                                null;

                                        status.setText(
                                                R.string.photo_control_completed
                                        );

                                        reason.setText(
                                                ""
                                        );

                                        progressText.setText(
                                                ""
                                        );

                                        setAllButtonsEnabled(
                                                false
                                        );

                                        return;
                                    }

                                    currentCheck =
                                            found;

                                    bindCheck(
                                            found
                                    );
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () ->
                                        openLogin()
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    status.setText(
                                            safe(
                                                    message,
                                                    getString(
                                                            R.string.photo_control_load_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void bindCheck(
            DriverPhotoCheckDto check
    ) {

        reason.setText(
                safe(
                        check.getReason(),
                        getString(
                                R.string.photo_control_reason_default
                        )
                )
        );

        int uploaded =
                check.getUploadedKindCount()
                        == null
                        ? 0
                        : Math.max(
                                0,
                                check.getUploadedKindCount()
                        );

        int required =
                check.getRequiredKindCount()
                        == null
                        ? 7
                        : Math.max(
                                1,
                                check.getRequiredKindCount()
                        );

        progressText.setText(
                getString(
                        R.string.photo_control_progress_format,
                        uploaded,
                        required
                )
        );

        String result =
                safe(
                        check.getResult(),
                        "pending"
                );

        boolean replacementAllowed =
                "failed".equals(
                        result
                )
                        ||
                        "manual_review".equals(
                                result
                        );

        Set<String> uploadedKinds =
                new HashSet<>();

        if (
                check.getUploadedKinds()
                        != null
        ) {

            uploadedKinds.addAll(
                    check.getUploadedKinds()
            );
        }

        if (
                !replacementAllowed
                        &&
                        pendingUploadKind != null
                        &&
                        uploadedKinds.contains(
                                pendingUploadKind
                        )
        ) {

            clearPendingUpload();
        }

        for (String kind : KINDS) {

            Button button =
                    buttons.get(
                            kind
                    );

            if (button == null) {
                continue;
            }

            boolean alreadyUploaded =
                    uploadedKinds.contains(
                            kind
                    );

            boolean canUpload =
                    !uploading
                            &&
                            (
                                    replacementAllowed
                                            ||
                                            !alreadyUploaded
                            );

            button.setEnabled(
                    canUpload
            );

            if (
                    pendingUploadKind != null
                            &&
                            pendingUploadKind.equals(
                                    kind
                            )
                            &&
                            pendingUploadFile != null
                            &&
                            pendingUploadFile.isFile()
            ) {

                button.setEnabled(
                        !uploading
                );

                button.setText(
                        getString(
                                R.string.photo_control_retry_upload_format,
                                labelForKind(
                                        kind
                                )
                        )
                );

            }
            else if (
                    alreadyUploaded
                            &&
                            !replacementAllowed
            ) {

                button.setText(
                        getString(
                                R.string.photo_control_uploaded_format,
                                labelForKind(
                                        kind
                                )
                        )
                );

            }
            else if (
                    alreadyUploaded
            ) {

                button.setText(
                        getString(
                                R.string.photo_control_retake_format,
                                labelForKind(
                                        kind
                                )
                        )
                );

            }
            else {

                button.setText(
                        getString(
                                R.string.photo_control_capture_format,
                                labelForKind(
                                        kind
                                )
                        )
                );
            }
        }

        status.setText(
                replacementAllowed
                        ? R.string.photo_control_retake_required
                        : R.string.photo_control_capture_required
        );
    }

    private void onPhotoAction(
            String kind
    ) {

        if (
                currentCheck == null
                        ||
                        uploading
        ) {
            return;
        }

        if (
                pendingUploadKind != null
                        &&
                        pendingUploadKind.equals(
                                kind
                        )
                        &&
                        pendingUploadFile != null
                        &&
                        pendingUploadFile.isFile()
        ) {

            uploadFile(
                    kind,
                    pendingUploadFile
            );

            return;
        }

        startCamera(
                kind
        );
    }

    private void startCamera(
            String kind
    ) {

        File directory =
                photoCacheDirectory();

        if (
                !directory.exists()
                        &&
                        !directory.mkdirs()
        ) {

            status.setText(
                    R.string.photo_control_camera_failed
            );

            return;
        }

        try {

            captureFile =
                    File.createTempFile(
                            "capture_",
                            ".jpg",
                            directory
                    );

            captureKind =
                    kind;

            Uri uri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            captureFile
                    );

            takePicture.launch(
                    uri
            );

        }
        catch (
                android.content.ActivityNotFoundException
                        |
                        IOException
                        |
                        IllegalArgumentException error
        ) {

            deleteQuietly(
                    captureFile
            );

            captureFile =
                    null;

            captureKind =
                    null;

            status.setText(
                    R.string.photo_control_camera_failed
            );
        }
    }

    private void onCameraResult(
            boolean success
    ) {

        if (
                !success
                        ||
                        captureFile == null
                        ||
                        captureKind == null
        ) {

            deleteQuietly(
                    captureFile
            );

            captureFile =
                    null;

            captureKind =
                    null;

            return;
        }

        File sanitized =
                null;

        try {

            sanitized =
                    File.createTempFile(
                            "sanitized_",
                            ".jpg",
                            photoCacheDirectory()
                    );

            PhotoSanitizer
                    .sanitizeJpeg(
                            captureFile,
                            sanitized
                    );

            deleteQuietly(
                    captureFile
            );

            captureFile =
                    null;

            pendingUploadKind =
                    captureKind;

            captureKind =
                    null;

            pendingUploadFile =
                    sanitized;

            uploadFile(
                    pendingUploadKind,
                    pendingUploadFile
            );

        }
        catch (
                IOException error
        ) {

            deleteQuietly(
                    sanitized
            );

            deleteQuietly(
                    captureFile
            );

            captureFile =
                    null;

            captureKind =
                    null;

            status.setText(
                    R.string.photo_control_processing_failed
            );
        }
    }

    private void uploadFile(
            String kind,
            File file
    ) {

        if (
                currentCheck == null
                        ||
                        file == null
                        ||
                        !file.isFile()
        ) {

            return;
        }

        uploading =
                true;

        progress.setVisibility(
                View.VISIBLE
        );

        status.setText(
                R.string.photo_control_uploading
        );

        bindCheck(
                currentCheck
        );

        repository.uploadPhotoCheckImage(
                currentCheck,
                kind,
                file,
                new DriverV1Repository.UploadCallback() {

                    @Override
                    public void onSuccess(
                            PrivateResourceDto resource
                    ) {

                        runOnUiThread(
                                () -> {

                                    uploading =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    clearPendingUpload();

                                    status.setText(
                                            R.string.photo_control_upload_success
                                    );

                                    loadCheck();
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () ->
                                        openLogin()
                        );
                    }

                    @Override
                    public void onConflict() {

                        runOnUiThread(
                                () -> {

                                    uploading =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    clearPendingUpload();

                                    status.setText(
                                            R.string.photo_control_changed_reload
                                    );

                                    loadCheck();
                                }
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    uploading =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    if (httpStatus != 0) {

                                        clearPendingUpload();
                                    }

                                    status.setText(
                                            safe(
                                                    message,
                                                    getString(
                                                            R.string.photo_control_upload_failed
                                                    )
                                            )
                                    );

                                    /*
                                     * При network failure сначала
                                     * перечитываем server state:
                                     * PUT мог успеть завершиться до
                                     * потери ответа.
                                     */
                                    if (httpStatus == 0) {

                                        loadCheck();

                                    }
                                    else if (
                                            currentCheck != null
                                    ) {

                                        bindCheck(
                                                currentCheck
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private void clearPendingUpload() {

        deleteQuietly(
                pendingUploadFile
        );

        pendingUploadFile =
                null;

        pendingUploadKind =
                null;
    }

    private void setAllButtonsEnabled(
            boolean enabled
    ) {

        for (Button button : buttons.values()) {

            if (button != null) {

                button.setEnabled(
                        enabled
                );
            }
        }
    }

    private String labelForKind(
            String kind
    ) {

        switch (kind) {

            case "left":
                return getString(
                        R.string.photo_kind_left
                );

            case "front":
                return getString(
                        R.string.photo_kind_front
                );

            case "right":
                return getString(
                        R.string.photo_kind_right
                );

            case "rear":
                return getString(
                        R.string.photo_kind_rear
                );

            case "open_trunk":
                return getString(
                        R.string.photo_kind_open_trunk
                );

            case "rear_cabin":
                return getString(
                        R.string.photo_kind_rear_cabin
                );

            case "front_cabin":
                return getString(
                        R.string.photo_kind_front_cabin
                );

            default:
                return kind;
        }
    }

    private File photoCacheDirectory() {

        return new File(
                getCacheDir(),
                "photo-control"
        );
    }

    private void cleanupOldPhotoCache() {

        File directory =
                photoCacheDirectory();

        File[] files =
                directory.listFiles();

        if (files == null) {
            return;
        }

        long cutoff =
                System.currentTimeMillis()
                        -
                        24L
                                *
                                60L
                                *
                                60L
                                *
                                1000L;

        for (File file : files) {

            if (
                    file != null
                            &&
                            file.isFile()
                            &&
                            file.lastModified()
                                    < cutoff
            ) {

                deleteQuietly(
                        file
                );
            }
        }
    }

    private void deleteQuietly(
            File file
    ) {

        if (
                file != null
                        &&
                        file.exists()
        ) {

            // Private cache cleanup only.
            file.delete();
        }
    }

    private void openLogin() {

        clearPendingUpload();

        deleteQuietly(
                captureFile
        );


        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private String safe(
            String value,
            String fallback
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return fallback;
        }

        return value.trim();
    }
}
