package com.whatiread.shared.suggest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class SuggestRanker {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");

    private SuggestRanker() {
    }

    public static List<SuggestCandidate> rerank(String query, List<SuggestCandidate> candidates, int limit) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return candidates.stream().limit(limit).toList();
        }

        double minSimilarity = minSimilarity(normalizedQuery.length());
        List<ScoredSuggestion> scored = new ArrayList<>(candidates.size());
        Map<String, Integer> tokenFrequency = new HashMap<>();

        for (int index = 0; index < candidates.size(); index++) {
            SuggestCandidate candidate = candidates.get(index);
            TokenMatch bestMatch = bestTokenMatch(normalizedQuery, candidate);
            if (bestMatch.similarity() < minSimilarity) {
                continue;
            }
            tokenFrequency.merge(bestMatch.token(), 1, Integer::sum);
            scored.add(new ScoredSuggestion(candidate, bestMatch, index));
        }

        scored.sort(Comparator
                .<ScoredSuggestion>comparingDouble(scoredSuggestion -> -scoredSuggestion.match().similarity())
                .thenComparingInt(scoredSuggestion -> -tokenFrequency.getOrDefault(scoredSuggestion.match().token(), 0))
                .thenComparingInt(ScoredSuggestion::sourceIndex)
                .thenComparing(scoredSuggestion -> scoredSuggestion.candidate().label()));

        return scored.stream()
                .limit(limit)
                .map(ScoredSuggestion::candidate)
                .toList();
    }

    private static TokenMatch bestTokenMatch(String query, SuggestCandidate candidate) {
        double bestSimilarity = -1.0;
        String bestToken = "";
        for (String token : candidate.tokens()) {
            double similarity = scoreToken(query, token, candidate.label());
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestToken = token;
            }
        }
        return new TokenMatch(bestToken, bestSimilarity);
    }

    private static double scoreToken(String query, String token, String label) {
        double similarity;
        if (token.startsWith(query)) {
            similarity = 1.0 + (double) query.length() / token.length() * 0.01;
        } else {
            similarity = jaroWinkler(query, token);
        }
        return Math.max(0.0, similarity - scriptPenalty(query, label));
    }

    private static double minSimilarity(int queryLength) {
        if (queryLength <= 3) {
            return 0.70;
        }
        if (queryLength <= 5) {
            return 0.78;
        }
        return 0.82;
    }

    public static List<String> tokenize(String value) {
        List<String> tokens = new ArrayList<>();
        var matcher = TOKEN_PATTERN.matcher(normalize(value));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static double scriptPenalty(String query, String label) {
        boolean queryIsAscii = query.chars().allMatch(ch -> ch < 128);
        boolean labelHasNonAscii = label.chars().anyMatch(ch -> ch > 127);
        return queryIsAscii && labelHasNonAscii ? 0.15 : 0.0;
    }

    private static double jaroWinkler(String left, String right) {
        if (left.equals(right)) {
            return 1.0;
        }
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        int leftLength = left.length();
        int rightLength = right.length();
        int matchDistance = Math.max(leftLength, rightLength) / 2 - 1;

        boolean[] leftMatches = new boolean[leftLength];
        boolean[] rightMatches = new boolean[rightLength];
        int matches = 0;

        for (int i = 0; i < leftLength; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, rightLength);
            for (int j = start; j < end; j++) {
                if (rightMatches[j] || left.charAt(i) != right.charAt(j)) {
                    continue;
                }
                leftMatches[i] = true;
                rightMatches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        int transpositions = 0;
        int rightIndex = 0;
        for (int i = 0; i < leftLength; i++) {
            if (!leftMatches[i]) {
                continue;
            }
            while (!rightMatches[rightIndex]) {
                rightIndex++;
            }
            if (left.charAt(i) != right.charAt(rightIndex)) {
                transpositions++;
            }
            rightIndex++;
        }

        double jaro = ((double) matches / leftLength
                + (double) matches / rightLength
                + (matches - transpositions / 2.0) / matches) / 3.0;

        int prefix = 0;
        int prefixLimit = Math.min(4, Math.min(leftLength, rightLength));
        for (int i = 0; i < prefixLimit; i++) {
            if (left.charAt(i) != right.charAt(i)) {
                break;
            }
            prefix++;
        }

        return jaro + prefix * 0.1 * (1.0 - jaro);
    }

    private record TokenMatch(String token, double similarity) {
    }

    private record ScoredSuggestion(SuggestCandidate candidate, TokenMatch match, int sourceIndex) {
    }
}
