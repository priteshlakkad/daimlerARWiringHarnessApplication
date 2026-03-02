package com.harness.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public abstract class S3ServiceBase {
    public abstract void upload(String key, MultipartFile file) throws Exception;
    public abstract List<String> listKeys(String harnessId);
    public abstract String presign(String key, int ttlSeconds);
}

