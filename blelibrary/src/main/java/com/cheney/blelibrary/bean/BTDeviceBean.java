package com.cheney.blelibrary.bean;

/**
 * Created by wangshuwen on 2017/3/28.
 */

public class BTDeviceBean  {

    private String name;

    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BTDeviceBean that = (BTDeviceBean) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return address != null ? address.equals(that.address) : that.address == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "BTDeviceBean{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
