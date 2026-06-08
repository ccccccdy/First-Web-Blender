package com.jin.blender.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ReviewSupport {

    public static final String NO_EXTRACTION_CODE = "无";

    private static final Set<String> QUARK_HOSTS = new HashSet<>(Arrays.asList(
            "pan.quark.cn",
            "drive.quark.cn"
    ));

    private ReviewSupport() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    public static boolean isValidQuarkUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return false;
        }

        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            return host != null && QUARK_HOSTS.contains(host.toLowerCase());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String validateDraftQuarkInfo(String quarkUrl) {
        String trimmedUrl = trimToNull(quarkUrl);
        if (trimmedUrl == null) {
            return null;
        }
        return isValidQuarkUrl(trimmedUrl) ? null : "Invalid Quark link";
    }

    public static String validateReviewSubmission(String quarkUrl, String extractionCode) {
        if (!isValidQuarkUrl(quarkUrl)) {
            return "A valid Quark link is required";
        }
        if (isBlank(extractionCode)) {
            return "Extraction code is required";
        }
        return null;
    }
}
