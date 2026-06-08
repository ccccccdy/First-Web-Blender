package com.jin.blender.support;

public final class ReviewStatus {

    public static final String PENDING_LINK = "PENDING_LINK";
    public static final String SYNCING = "SYNCING";
    public static final String SYNCED = "SYNCED";
    public static final String SYNC_FAILED = "SYNC_FAILED";

    private ReviewStatus() {
    }

    public static String normalize(String status) {
        if (status == null || status.trim().isEmpty()) {
            return PENDING_LINK;
        }
        return status.trim();
    }

    public static boolean isPendingLink(String status) {
        return PENDING_LINK.equals(normalize(status));
    }

    public static boolean isSyncing(String status) {
        return SYNCING.equals(normalize(status));
    }

    public static boolean isSynced(String status) {
        return SYNCED.equals(normalize(status));
    }

    public static boolean isSyncFailed(String status) {
        return SYNC_FAILED.equals(normalize(status));
    }

    public static boolean canEdit(String status) {
        String normalized = normalize(status);
        return PENDING_LINK.equals(normalized) || SYNC_FAILED.equals(normalized);
    }
}
