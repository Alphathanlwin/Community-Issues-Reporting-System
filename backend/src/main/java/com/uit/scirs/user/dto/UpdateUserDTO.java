package com.uit.scirs.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUserDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String profileImageUrl;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
