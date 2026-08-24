package com.nordairemapper.service.shizuku;

// Runs inside the Shizuku server process (shell-level privileges) when Keyforge
// binds it for the Unlock flow. Keep the interface stable: bump
// ShizukuGrant.SERVICE_VERSION on any change.
interface IGrantService {
    int runCommand(String command);
    void exit();
}
