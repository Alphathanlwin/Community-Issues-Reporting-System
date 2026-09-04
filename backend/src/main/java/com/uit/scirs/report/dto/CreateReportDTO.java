package com.uit.scirs.report.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateReportDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    private BigDecimal longitude;

    private String addressText;

    // Second-call fields for the duplicate-check round trip (see
    // DuplicateDetectionService): the citizen either confirms an existing
    // report is the same issue, or explicitly says "no, this is different"
    // and forces a new one. Neither is set on the first attempt.
    private Long confirmDuplicateOfId;

    private Boolean forceCreate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public Long getConfirmDuplicateOfId() {
        return confirmDuplicateOfId;
    }

    public void setConfirmDuplicateOfId(Long confirmDuplicateOfId) {
        this.confirmDuplicateOfId = confirmDuplicateOfId;
    }

    public Boolean getForceCreate() {
        return forceCreate;
    }

    public void setForceCreate(Boolean forceCreate) {
        this.forceCreate = forceCreate;
    }
}
