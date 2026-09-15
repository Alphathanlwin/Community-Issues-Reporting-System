package com.uit.scirs.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    // Myanmar mobile only: fixed "+959" prefix + the 9-digit subscriber
    // number (see PhoneInput on the frontend).
    @Pattern(regexp = "^\\+959[0-9]{9}$", message = "Enter a 9-digit phone number")
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
