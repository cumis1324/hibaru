package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.MainActivity.context;
// 
// 
// 
// 

import android.content.Context;

import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.IndexLink;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;

import java.util.List;

public class IndexUtils{

    public static boolean refreshIndex(Context mContext , IndexLink indexLink) {
        Thread thread = null;
//        if(!deleteIndex(indexLink)){
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String folderType = indexLink.getFolderType();
                    String indexType = indexLink.getIndexType();

                    String link =indexLink.getLink();
                    String user =indexLink.getUsername();
                    String pass =indexLink.getPassword();

                    System.out.println("Before setting id"+ indexLink.getId());

//                    int id = indexLink.getId();
//                    DatabaseClient.getInstance(context).getAppDatabase().indexLinksDao().deleteById(indexLink.getId());

                    IndexLink indexLinkAgain = DatabaseClient.getInstance(mContext).getAppDatabase().indexLinksDao().find(link);
                    if(indexLinkAgain==null){
                        DatabaseClient.getInstance(mContext).getAppDatabase().indexLinksDao().insert(indexLink);
                    }
                    int id =indexLinkAgain.getId();

                    System.out.println("After setting id"+ indexLinkAgain.getId());


                    if(folderType.equals("Movies")) {
                        if(indexType.equals("GDIndex")) {
                            // 
                        }
                        if(indexType.equals("GoIndex")) {
                            // 
                        }
                        if(indexType.equals("MapleIndex")){
                            // 
                        }
                        if(indexType.equals("SimpleProgram")){
                            // 
                        }
                    }

                    if(folderType.equals("TVShows")){
                        if(indexType.equals("GDIndex")) {
                            // 
                        }
                        if(indexType.equals("GoIndex")) {
                            // 
                        }
                        if(indexType.equals("MapleIndex")){
                            // 
                        }
                        if(indexType.equals("SimpleProgram")){
                            // 
                        }
                    }

                }
            });
            thread.start();
            return thread.isAlive();
//        }
//        return thread.isAlive();
    }

    public static boolean deleteIndex(Context mContext,IndexLink indexLink) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (indexLink.getFolderType().equals("Movies")) {
                    DatabaseClient.getInstance(mContext)
                            .getAppDatabase()
                            .movieDao()
                            .deleteAllFromthisIndex(indexLink.getId());
                }
                if (indexLink.getFolderType().equals("TVShows")) {
                    DatabaseClient.getInstance(mContext)
                            .getAppDatabase()
                            .episodeDao()
                            .deleteAllFromThisIndex(indexLink.getId());


                    List<TVShowSeasonDetails> seasonsList = DatabaseClient
                            .getInstance(mContext)
                            .getAppDatabase()
                            .tvShowSeasonDetailsDao()
                            .getAll();

                    for(TVShowSeasonDetails season : seasonsList) {
                        List<Episode> episodeList = DatabaseClient
                                .getInstance(mContext)
                                .getAppDatabase()
                                .episodeDao()
                                .getFromSeasonOnly(season.getId());
                        if(episodeList==null || episodeList.size()==0){
                            DatabaseClient.getInstance(mContext).getAppDatabase().tvShowSeasonDetailsDao().deleteById(season.getId());
                        }
                    }

                    List<TVShow> tvShowList = DatabaseClient
                            .getInstance(mContext)
                            .getAppDatabase()
                            .   tvShowDao().getAll();

                    for(TVShow tvShow : tvShowList) {
                        List<TVShowSeasonDetails> seasonsInThisShow = DatabaseClient
                                .getInstance(mContext)
                                .getAppDatabase()
                                .tvShowSeasonDetailsDao()
                                .findByShowId(tvShow.getId());
                        if(seasonsInThisShow==null|| seasonsInThisShow.size()==0){
                            DatabaseClient.getInstance(mContext).getAppDatabase().tvShowDao().deleteById(tvShow.getId());
                        }
                    }

                }
                DatabaseClient.getInstance(mContext)
                        .getAppDatabase()
                        .indexLinksDao()
                        .deleteById(indexLink.getId());
            }
        });
        thread.start();
        try{
            thread.join();
        }catch (InterruptedException e){
            System.out.println(e.toString());
        }

        return thread.isAlive();
    }

    static int noOfMedia = 0;
    public static int getNoOfMedia(Context mContext,IndexLink t) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                noOfMedia = 0;
                if(t.getFolderType()!=null && t.getFolderType().equals("Movies")){
                    noOfMedia = DatabaseClient.getInstance(mContext).getAppDatabase().movieDao().getNoOfMovies(t.getId());
                    System.out.println("noOfMedia after calculation"+noOfMedia);
                }
                if(t.getFolderType()!=null && t.getFolderType().equals("TVShows")){
                    noOfMedia = DatabaseClient.getInstance(mContext).getAppDatabase().episodeDao().getNoOfShows(t.getId());
                    System.out.println("noOfMedia after calculation"+noOfMedia);
                }
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("noOfMedia after calculation"+noOfMedia);


        return noOfMedia;
    }

    public static void disableIndex(Context mContext,IndexLink indexLink){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(indexLink.getFolderType().equals("Movies")){
                    DatabaseClient.getInstance(mContext).getAppDatabase().movieDao().disableFromThisIndex(indexLink.getId());
                }
                if(indexLink.getFolderType().equals("TVShows")){
                   DatabaseClient.getInstance(mContext).getAppDatabase().episodeDao().disableFromThisIndex(indexLink.getId());
                }
                DatabaseClient.getInstance(mContext).getAppDatabase().indexLinksDao().disableIndex(indexLink.getId());

            }
        });
        thread.start();

    }

    public static void enableIndex(Context mContext,IndexLink indexLink){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(indexLink.getFolderType().equals("Movies")){
                    DatabaseClient.getInstance(mContext).getAppDatabase().movieDao().enableFromThisIndex(indexLink.getId());
                }
                if(indexLink.getFolderType().equals("TVShows")){
                   DatabaseClient.getInstance(mContext).getAppDatabase().episodeDao().enableFromThisIndex(indexLink.getId());
                    System.out.println("noOfMedia after calculation"+noOfMedia);
                }
                DatabaseClient.getInstance(mContext).getAppDatabase().indexLinksDao().enableIndex(indexLink.getId());

            }
        });
        thread.start();
    }
}
