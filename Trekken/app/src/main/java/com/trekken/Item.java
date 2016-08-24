package com.trekken;

public class Item implements Comparable<Item> {

    private String _name;
    private String _data;
    private String _date;
    private String _path;
    private String _image;

    public Item(String name, String data, String date, String path, String image) {
        _name = name;
        _data = data;
        _date = date;
        _path = path;
        _image = image;
    }

    public String getName() {
        return _name;
    }

    public String getData() {
        return _data;
    }

    public String getDate() {
        return _date;
    }

    public String getPath() {
        return _path;
    }

    public String getImage() {
        return _image;
    }

    public int compareTo(Item object) {
        if (this._name != null)
            return this._name.toLowerCase().compareTo(object.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }

}

