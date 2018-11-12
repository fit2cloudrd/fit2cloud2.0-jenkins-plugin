package com.fit2cloud.codedeploy.client.model;

public class Page {
    private Object listObject;
    private Object param;
    private long itemCount;
    private long pageCount;

    public Object getListObject() {
        return listObject;
    }

    public void setListObject(Object listObject) {
        this.listObject = listObject;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public long getPageCount() {
        return pageCount;
    }

    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }
}
