package com.example.pos.compliance.synchronization;

public interface EtimsSynchronizer {

    String getSyncType();

    SyncResult sync();

    record SyncResult(int synced, int failed, String error) {
        public boolean isSuccess() { return error == null && failed == 0; }
    }
}
