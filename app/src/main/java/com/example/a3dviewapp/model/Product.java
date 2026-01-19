package com.example.a3dviewapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Product {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("image")
    private String imageUrl; // This is PREVIEW image

    @SerializedName("type")
    private String type;

    @SerializedName("files")
    private List<FileInfo> files; // Add this

    // Inner class for file info
    public static class FileInfo {
        @SerializedName("id")
        private String id;

        @SerializedName("url")
        private String url;

        @SerializedName("file_name")
        private String fileName;

        // Getters
        public String getId() { return id; }
        public String getUrl() { return url; }
        public String getFileName() { return fileName; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<FileInfo> getFiles() { return files; }
    public void setFiles(List<FileInfo> files) { this.files = files; }

    // NEW METHOD: Get actual PNG file URL
    public String getActualImageUrl() {
        if (files != null && !files.isEmpty()) {
            // Return the first file's URL (actual PNG)
            return files.get(0).getUrl();
        }
        // Fallback to preview image if no files
        return imageUrl;
    }

    // NEW METHOD: Get file name
    public String getActualFileName() {
        if (files != null && !files.isEmpty()) {
            return files.get(0).getFileName();
        }
        return null;
    }
}