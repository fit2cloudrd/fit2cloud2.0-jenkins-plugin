package com.fit2cloud.codedeploy2.oss;

/**
 * Created by linjinbo on 2017/10/22.
 */
public class S3Proxy {
    private String address;
    private String port;
    private String userName;
    private String password;

    public S3Proxy(String address, String port, String userName, String password){
        this.address = address;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
