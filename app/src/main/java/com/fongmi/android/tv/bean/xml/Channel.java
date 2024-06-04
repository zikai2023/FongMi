package com.fongmi.android.tv.bean.xml;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "channel")
public class Channel {
    @Attribute(name = "id")
    private String id;

    @Element(name = "display-name")
    private String displayName;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // getters and setters
}
