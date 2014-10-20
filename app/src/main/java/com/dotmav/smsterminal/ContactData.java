package com.dotmav.smsterminal;

public class ContactData {
    private String full_name;
    private String number;

    public ContactData(String full_name, String number){
        this.full_name = full_name;
        this.number = number;
    }

    public String getFullName(){
        return full_name;
    }

    public String getNumber(){
        return number;
    }

    public String toString(){
        return full_name + "|" + number;
    }
}
