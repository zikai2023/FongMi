package com.fongmi.android.tv.player;


import android.net.Uri;

import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


public class EpgUtil implements Download.Callback {
    private static Map<String, String> channelDisplayNames = new HashMap<>();
    private static Map<String, Epg> epgMap = new HashMap<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    // ... 其他代码 ...


    public Map<String, Epg> parseEpgFromXmlSource(String xmlUri)  {
        String xmlStream;
        if (isRemoteUrl(xmlUri)) {
            Live live = new Live();
            xmlStream = OkHttp.string(xmlUri);
        } else {
            xmlStream = fetchXmlFromLocalResource(xmlUri);
        }

        return extractedEpgXmlStream(xmlStream);
    }

    public Map<String, Epg> parseEpgFromXmlSource(Live live)  {
        String epg_xml_url = live.getCatchup().getTvgUrl();
        String epg_local_name = live.getName();

        File file = new File(Path.cache(), Uri.parse(epg_xml_url).getLastPathSegment());

        String xmlStream = Path.read(file);

        return extractedEpgXmlStream(xmlStream);
    }

    private Map<String, Epg> extractedEpgXmlStream(String xmlStream) {
        Document document = null;
        try {

            document = DocumentHelper.parseText(xmlStream);


            Element rootElement = document.getRootElement();
            List<Element> channelElements = rootElement.elements("channel");
            for (Element channel : channelElements) {
                String channelId = channel.attributeValue("id");
                String displayName = channel.elementText("display-name");
                if (displayName != null) {
                    channelDisplayNames.put(channelId, displayName);
                }
            }


            List<Element> programmeElements = rootElement.elements("programme");
            for (Element programme : programmeElements) {
                String start = programme.attributeValue("start");
                String stop = programme.attributeValue("stop");
                String channel = programme.attributeValue("channel");

                // 获取<title>元素
                Element titleElement = programme.element("title");
                String titleLang = null;
                String titleText = null;
                if (titleElement != null) {
                    titleLang = titleElement.attributeValue("lang");
                    titleText = titleElement.getTextTrim();
                }

                // 创建EpgData对象
                EpgData epgData = new EpgData();
                epgData.setTitle(titleText);

                SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm");
                String starttimeStr = timeFormat.format(parseDateTime(start));
                String endtimeStr = timeFormat.format(parseDateTime(stop));
                epgData.setStart(starttimeStr);
                epgData.setEnd(endtimeStr);
                epgData.setStartTime(parseDateTime(start).getTime());
                epgData.setEndTime(parseDateTime(stop).getTime());


                // 获取或创建Epg对象
                String channelName = channelDisplayNames.get(channel);
                Epg epg = epgMap.get(channelName);
                if (epg == null) {
                    epg = new Epg();
                    epg.setKey(channelName);
                    epg.setList(new ArrayList<>());
                    epgMap.put(channelName, epg);
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                epg.setDate(dateFormat.format(parseDateTime(start)));
                epg.getList().add(epgData);

            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return epgMap;
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
