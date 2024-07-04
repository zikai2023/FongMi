package com.fongmi.android.tv.player.extractor;

import android.content.Context;
import android.os.SystemClock;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.bean.DownloadTask;
import com.fongmi.android.tv.player.DownloadSource;
import com.github.catvod.net.OkHttp;
import com.github.jadehh.m3u8.M3U8Class;
import com.github.jadehh.m3u8.M3U8Module;
import com.orhanobut.logger.Logger;


import java.util.ArrayList;
import java.util.List;


public class M3U8 implements DownloadSource.Extractor {

    private M3U8Class m3u8;
    public M3U8(){
    }
    public void init(Context context){
        if (m3u8 == null){
            m3u8 = new M3U8Class(context);
        }
    }
    @Override
    public boolean downloadMatch(String scheme, String host,String url) {
        if ( ("http".equals(scheme) || "https".equals(scheme) ) && url.contains(".m3u8")) return true ;
        else return false;
    }

    @Override
    public boolean match(String scheme, String host) {
        return false;
    }

    @Override
    public String fetch(String url) throws Exception {
        return null;
    }

    @Override
    public List<DownloadTask> startDownload(String name, String url, String thumbPath) {
        DownloadTask task = new DownloadTask();
        if (name.length() == 0) name = "m3u8_video.mp4";
        task.setTaskId(m3u8.startDownload(url));
        task.setTaskType(Constant.M3U8_DOWNLOAD_TYPE);
        task.setFileName(name);
        task.setUrl(url);
        task.setThumbnailPath(thumbPath);
        task.setTaskStatus(Constant.DOWNLOAD_CONNECTION);
        List <DownloadTask> tasks = new ArrayList<>();
        tasks.add(task);
        return tasks;
    }

    @Override
    public DownloadTask getDownloadingTask(DownloadTask task) {
        M3U8Module m3U8Module = m3u8.getDownloadTask(task.getTaskId());
        if (m3U8Module.getTaskType() == 1) {
            task.setTaskStatus(Constant.DOWNLOAD_SUCCESS);
            task.setFileSize(task.getDownloadSize());
        }
        else if (m3U8Module.getTaskType() == 3 && task.getTaskStatus() != Constant.DOWNLOAD_CONNECTION){
            task.setTaskStatus(Constant.DOWNLOAD_STOP);
        }
        else if (m3U8Module.getTaskType() == 4) task.setTaskStatus(Constant.DOWNLOAD_LOADING);
        task.setLocalPath(m3U8Module.getFilePath());
        task.setDownloadSpeed(m3U8Module.getSpeed());
        task.setDownloadSize(m3U8Module.getDownloadSize());
        task.setPercent(m3U8Module.getPercent());
        task.update();
        return task;
    }


    public long getFileSize(int percent, long downloadSize) {
        if (percent > 0 ){
            return downloadSize / percent * 100;
        }
        return 0;
    }

    @Override
    public List<DownloadTask> resumeDownload(DownloadTask task) {
        m3u8.resumeDownload(task.getTaskId());
        task.setTaskStatus(Constant.DOWNLOAD_CONNECTION);
        List <DownloadTask> tasks = new ArrayList<>();
        tasks.add(task);
        return tasks;
    }

    @Override
    public void stopDownload(DownloadTask task) {
        m3u8.stopDownload(task.getTaskId());
        task.setTaskStatus(Constant.DOWNLOAD_STOP);
        task.update();
    }

    @Override
    public void delete(DownloadTask task) {
        m3u8.deleteDownload(task.getTaskId());
    }

    @Override
    public void stop() {

    }

    @Override
    public void exit() {

    }
}
