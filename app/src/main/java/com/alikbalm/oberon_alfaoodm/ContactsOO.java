package com.alikbalm.oberon_alfaoodm;

import com.orm.SugarRecord;

public class ContactsOO extends SugarRecord {
    public ContactsOO(){}

    String contactId, userName, firstName, lastName, email, department, avatar, profileUrl;

    public ContactsOO(String contactId, String userName, String firstName, String lastName, String email, String department, String avatar, String profileUrl) {
        this.contactId = contactId;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.avatar = avatar;
        this.profileUrl = profileUrl;
    }
}
