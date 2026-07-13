package com.whatiread.instance.service;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.instance.api.SetupAdminRequest;

public interface SetupService {

    AuthResponse createAdmin(SetupAdminRequest request);
}
