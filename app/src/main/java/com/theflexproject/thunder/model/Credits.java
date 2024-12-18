package com.theflexproject.thunder.model;

import java.util.List;

public class Credits {
    private List<Cast> castList;
    private List<Crew> crewList;

    // Constructor
    public Credits(List<Cast> castList, List<Crew> crewList) {
        this.castList = castList;
        this.crewList = crewList;
    }

    // Getters
    public List<Cast> getCastList() {
        return castList;
    }

    public List<Crew> getCrewList() {
        return crewList;
    }
}

