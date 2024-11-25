package com.theflexproject.thunder.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Movie> movieMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<TVShow> tvMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<TVShowSeasonDetails> seasonMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Episode> episodeMutableLiveData = new MutableLiveData<>();

    public LiveData<Movie> getMovie() {
        return movieMutableLiveData;
    }
    public LiveData<TVShow> getTVShow() {
        return tvMutableLiveData;
    }
    public LiveData<TVShowSeasonDetails> getSeason() {
        return seasonMutableLiveData;
    }
    public LiveData<Episode> getEpisode() {
        return episodeMutableLiveData;
    }

    public void setMovieMutableLiveData(Movie model) {
        movieMutableLiveData.setValue(model);
    }
    public void setTvMutableLiveData(TVShow model) {
        tvMutableLiveData.setValue(model);
    }
    public void setSeasonMutableLiveData(TVShowSeasonDetails model) {
        seasonMutableLiveData.setValue(model);
    }
    public void setEpisodeMutableLiveData(Episode model) {
        episodeMutableLiveData.setValue(model);
    }
}


