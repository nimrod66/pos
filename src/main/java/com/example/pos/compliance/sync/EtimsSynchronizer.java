package com.example.pos.compliance.sync;

public interface EtimsSynchronizer {
    String getSyncType();
    SyncResult sync();

    record SyncResult(int synced, int failed, String error) {
        public boolean isSuccess() { return error == null; }
    }
}