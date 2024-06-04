package com.fongmi.android.tv.bean.xml;


import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.Date;

@Root(name = "programme")
public class Programme {
    @Attribute(name = "start")
    private String start;

    @Attribute(name = "stop")
    private String stop;

    @Attribute(name = "channel")
    private String channel;

    @Element(name = "title")
    private String title;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public String getChannel() {
        return channel;
    }


    public String getTitle() {
        return title;
    }
}