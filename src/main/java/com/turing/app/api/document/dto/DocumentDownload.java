package com.turing.app.api.document.dto;

public record DocumentDownload(String originalName, String mimeType, byte[] content) {}
