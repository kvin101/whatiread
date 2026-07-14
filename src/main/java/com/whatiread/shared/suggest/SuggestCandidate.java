package com.whatiread.shared.suggest;

import java.util.List;

public record SuggestCandidate(String key, String label, List<String> tokens) {
}
