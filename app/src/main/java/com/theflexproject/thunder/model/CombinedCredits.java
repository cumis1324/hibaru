package com.theflexproject.thunder.model;

import java.util.List;

public class CombinedCredits implements MyMedia {
    private List<MovieCredit> movieCredits;
    private List<TvCredit> tvCredits;

    public CombinedCredits(List<MovieCredit> movieCredits, List<TvCredit> tvCredits) {
        this.movieCredits = movieCredits;
        this.tvCredits = tvCredits;
    }

    // Getters
    public List<MovieCredit> getMovieCredits() {
        return movieCredits;
    }

    public List<TvCredit> getTvCredits() {
        return tvCredits;
    }
}
