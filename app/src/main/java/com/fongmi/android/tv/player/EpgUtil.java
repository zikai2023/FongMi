package com.fongmi.android.tv.player;



import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.github.catvod.net.OkHttp;
import com.google.common.io.CharStreams;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


public class EpgUtil {
    private static Map<String, String> channelDisplayNames = new HashMap<>();
    private static Map<String, Epg> epgMap = new HashMap<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    // ... 其他代码 ...


    public Map<String, Epg> parseEpgFromXmlSource(String xmlUri)  {


        String xmlStream;
        if (isRemoteUrl(xmlUri)) {
            xmlStream = OkHttp.string(xmlUri);
        } else {
            xmlStream = fetchXmlFromLocalResource(xmlUri);
        }

        Document document = null;
        try {
            // 读取XML文件
//            Document document = DocumentHelper.readDocument("path_to_your_xml_file.xml");
            // 或者，如果你有XML字符串，可以使用parseText()

            document = DocumentHelper.parseText(xmlStream);
        } catch (DocumentException e) {
            e.printStackTrace();
        }

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

            SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm");
            String starttimeStr = timeFormat.format(parseDateTime(start));
            String endtimeStr = timeFormat.format(parseDateTime(stop));
            epgData.setStart(starttimeStr);
            epgData.setTitle(titleText);
            epgData.setEnd(endtimeStr);


            // 获取或创建Epg对象
            String channelName = channelDisplayNames.get(channel);
            Epg epg = epgMap.get(channelName);
            if (epg == null) {
                epg = new Epg();
                epg.setKey(channel);
                epg.setList(new ArrayList<>());
                epgMap.put(channelName, epg);
            }
            epg.getList().add(epgData);

        }

        return epgMap;
    }

    private boolean isRemoteUrl(String xmlSource) {
        return xmlSource.startsWith("http://") || xmlSource.startsWith("https://");
    }



    private String fetchXmlFromLocalResource(String resourcePath)  {
        return "";
    }

    public Epg getEpgByChannelId(String channel) {
        String channelName = channelDisplayNames.get(channel);
        return epgMap.get(channelName);
    }

    // ... (previous implementation of EpgSaxHandler)

    // Helper method to parse datetime string into Date, then extract date part
    private Date parseDateTime(String dateTimeStr) {
        // Implement this method based on the actual datetime format in your XML
        // For example, using SimpleDateFormat
        try {
            return sdf.parse(dateTimeStr.substring(0, 14)); // Assuming date format is consistent
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
