package com.fongmi.android.tv.bean.xml;

import org.simpleframework.xml.*;

import java.util.Date;
import java.util.List;

@Root(name = "tv", strict = false)
public class Tv {


    @ElementList(entry = "channel", inline = true)
    private List<Channel> channels;

    @ElementList(entry = "programme", inline = true)
    private List<Programme> programmes;


    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public List<Programme> getProgrammes() {
        return programmes;
    }

    public void setProgrammes(List<Programme> programmes) {
        this.programmes = programmes;
    }


    // getters and setters



}


