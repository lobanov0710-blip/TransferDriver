package ru.transferservis.admin;

import android.app.Application;
import ru.transferservis.admin.data.AdminRepository;

public final class AdminApplication extends Application {
    private AdminRepository repository;
    @Override public void onCreate() { super.onCreate(); repository = new AdminRepository(this); }
    public AdminRepository repository() { return repository; }
}
