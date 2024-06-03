package com.fongmi.android.tv.test;

import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.player.EpgUtil;
import com.github.catvod.net.OkHttp;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.*;
public class EpgUtilTest {


    @Test
    public void testParseEpgFromXml() {

        String url = "https://epg.erw.cc/cc.xml";
        String xmlStream = OkHttp.string(url);

        EpgUtil epgUtil = new EpgUtil();
        Map<String, Epg> epgMap = epgUtil.parseEpgFromXmlSource(url);

        Epg epg = epgUtil.getEpgByChannelId("85");
        System.out.println(epg);

        Epg epg2 = epgUtil.getEpgByChannelId("10086");
        System.out.println(epg2);

    }
}