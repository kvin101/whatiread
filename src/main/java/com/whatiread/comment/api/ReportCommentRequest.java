package com.whatiread.comment.api;

import jakarta.validation.constraints.Size;

public record ReportCommentRequest(
        @Size(max = 500) String reason
) {
}
