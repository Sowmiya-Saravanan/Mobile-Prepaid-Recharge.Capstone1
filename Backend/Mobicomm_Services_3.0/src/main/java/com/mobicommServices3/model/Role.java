package com.mobicommServices3.model;

public enum Role {
    ADMIN("ROLE_ADMIN"), 
    SUBSCRIBER("ROLE_SUBSCRIBER");

    private final String authority;
    Role(String authority) { this.authority = authority; }
    public String getAuthority() { return authority; }
}