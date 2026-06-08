package com.jin.blender.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReviewSupportTest {

    @Test
    public void validateReviewSubmissionAllowsExplicitNoExtractionCode() {
        assertNull(ReviewSupport.validateReviewSubmission("https://pan.quark.cn/s/abc123", "无"));
    }

    @Test
    public void validateReviewSubmissionStillRejectsBlankExtractionCode() {
        assertEquals(
                "Extraction code is required",
                ReviewSupport.validateReviewSubmission("https://pan.quark.cn/s/abc123", "")
        );
    }
}
