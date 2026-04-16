package com.harness.dtos;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExternalChatResponse {
    private String session_id;
    private String message;
    private List<String> options;
    private List<Map<String,String>> sources;
    private boolean restart;
    private String pdf_url;

    
}
