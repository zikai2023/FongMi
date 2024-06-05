package com.fongmi.android.tv.player;


import android.net.Uri;

import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.xml.Tv;
import com.fongmi.android.tv.bean.xml.Channel;
import com.fongmi.android.tv.bean.xml.Programme;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;



public class EpgUtil implements Download.Callback {
    private static Map<String, String> channelDisplayNames = new HashMap<>();
    private static Map<String, Epg> epgMap = new HashMap<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    private SimpleDateFormat showTimeFormat = new SimpleDateFormat("H:mm");

    // ... 其他代码 ...

    public Map<String, Epg> parseEpgFromXmlSource(String xmlUri)  {
        if (xmlUri.isEmpty()) return epgMap;

        String xmlStream;
        if (isRemoteUrl(xmlUri)) {
            xmlStream = OkHttp.string(xmlUri);
        } else {
            xmlStream = fetchXmlFromLocalResource(xmlUri);
        }

        return fromXml(xmlStream);
    }

    public Map<String, Epg> parseEpgFromXmlSource(Live live)  {
        if(!checkLiveConfig(live)) return epgMap;

        String epg_xml_url = live.getCatchup().getTvgUrl();

        File file = new File(Path.cache(), Uri.parse(epg_xml_url).getLastPathSegment());
        String xmlStream = Path.read(file);

        Set<String> existChannelNames = new HashSet<>();
        for (Group g : live.getGroups()){
            List<com.fongmi.android.tv.bean.Channel> channels = g.getChannel();
            for (com.fongmi.android.tv.bean.Channel c : channels){
                String channelName = c.getTvgName();
                existChannelNames.add(channelName);
            }
        }

        return fromXml(xmlStream,existChannelNames);
    }

    private boolean checkLiveConfig(Live live) {
        String epg_xml_url = live.getCatchup().getTvgUrl();
        if(epg_xml_url == null || epg_xml_url.isEmpty())
        {
            return false;
        }
        return true;
    }

    private Map<String, Epg> fromXml(String xmlStream) {
        return fromXml(xmlStream, null);
    }

    private Map<String, Epg> fromXml(String xmlStream, Set<String> existChannelNames) {
        Persister persister = new Persister();
        Tv tv;
        try {
            tv = persister.read(Tv.class, xmlStream);

            for (Channel channel : tv.getChannels()) {
                String channelId = channel.getId();
                String displayName = channel.getDisplayName();

                if (displayName != null) {
                    channelDisplayNames.put(channelId, displayName);
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (Programme programme : tv.getProgrammes()) {
                String channel = programme.getChannel();
                String channelName = channelDisplayNames.get(channel);
                if(existChannelNames != null && !existChannelNames.contains(channelName) )
                {
                    continue;
                }
                String start = programme.getStart();
                String stop = programme.getStop();

                // 获取<title>元素的文本和语言
                String titleText = programme.getTitle();
                Date startDate = parseDateTime(start);
                Date endDate = parseDateTime(stop);

                EpgData epgData = new EpgData();
                epgData.setTitle(titleText);

                epgData.setStart(showTimeFormat.format(startDate));
                epgData.setEnd(showTimeFormat.format(endDate));
                epgData.setStartTime(startDate.getTime());
                epgData.setEndTime(endDate.getTime());


                Epg epg = epgMap.get(channelName);
                if (epg == null) {
                    epg = new Epg();
                    epg.setKey(channelName);
                    epg.setList(new ArrayList<>());
                    epgMap.put(channelName, epg);
                }

                epg.setDate(dateFormat.format(parseDateTime(start)));
                epg.getList().add(epgData);

            }

            return epgMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }


    private boolean isRemoteUrl(String xmlSource) {
        return xmlSource.startsWith("http://") || xmlSource.startsWith("https://");
    }

    private String fetchXmlFromLocalResource(String resourcePath)  {
        // 使用 Path.read 方法来读取文件内容
        String content = Path.read(resourcePath);
        return content;
    }

    public Map<String, Epg> getEpgMap() {

        return epgMap;
    }

    public Epg getEpgByChannelName(String channelName) {
        return epgMap.get(channelName);
    }

    public Epg getEpgByChannelId(String channel) {
        String channelName = channelDisplayNames.get(channel);
        return epgMap.get(channelName);
    }

    private Date parseDateTime(String dateTimeStr) {

        try {
            return sdf.parse(dateTimeStr.substring(0, 14)); // Assuming date format is consistent
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public  void download(Live live) {
        if(!checkLiveConfig(live))  return ;
        String epg_xml_url = live.getCatchup().getTvgUrl();


        File file = new File(Path.cache(), Uri.parse(epg_xml_url).getLastPathSegment());
        long lastModified = file.lastModified();
        long todayInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        if (lastModified < todayInMillis) {
            Download.create(epg_xml_url, file, this).start();
        }

    }

    @Override
    public void progress(int progress) {

    }

    @Override
    public void error(String msg) {
        Notify.show(msg);

    }

    @Override
    public void success(File file) {
        Notify.show("更新 x-tvg-url");

    }


}
