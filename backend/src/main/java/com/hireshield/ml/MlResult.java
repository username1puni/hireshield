package com.hireshield.ml;

import java.util.List;

public record MlResult(double probability, boolean flagged, List<TermContribution> topTerms) {

    /** A word or word pair and how much it pushed the score towards "fraud". */
    public record TermContribution(String term, double weight) {
    }
}
