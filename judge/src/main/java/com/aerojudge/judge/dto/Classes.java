package com.aerojudge.judge.dto;

public class Classes {
    
    public Classes(String _class) {
        this._class = _class;
    }

    private String _class;

    public String get_Class ()
    {
        return _class;
    }

    public void set_Class (String _class)
    {
        this._class = _class;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [class = "+_class+"]";
    }
}
