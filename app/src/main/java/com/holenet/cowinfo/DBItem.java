package com.holenet.cowinfo;

public class DBItem {
    private int id;
    private String name;
    private boolean device, server;

    public DBItem(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDevice() {
        return device;
    }

    public void setDevice(boolean device) {
        this.device = device;
    }

    public boolean isServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public void addProperty(DBItem db) {
        if(id==0)
            id = db.getId();
        device = device || db.isDevice();
        server = server || db.isServer();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DBItem && name.equals(((DBItem)o).getName());
    }
}
