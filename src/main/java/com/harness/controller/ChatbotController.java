package com.harness.controller;

import com.harness.dtos.ChatbotButton;
import com.harness.dtos.ChatbotRequest;
import com.harness.dtos.ChatbotResponse;
import com.harness.dtos.ExternalChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chatbot")
@Log4j2
@Tag(name = "Chatbot", description = "Endpoints for chatbot interaction")
@RequiredArgsConstructor
public class ChatbotController {

        private final RestTemplate restTemplate;
        private static final String EXTERNAL_CHAT_API_URL = "http://65.2.143.213:8010/chat";

        @PostMapping("/query")
        @Operation(summary = "Query the chatbot", description = "Forwards the query to the external chatbot API and returns the mapped response.")
        public ResponseEntity<ChatbotResponse> query(@RequestBody ChatbotRequest request) {
                log.info("Received chatbot query for VIN: {}, query: {}", request.getVin(), request.getQuery());

                Map<String, String> externalRequest = new HashMap<>();
                externalRequest.put("session_id", request.getVin());
                externalRequest.put("user_message", request.getQuery());

                try {
                        ResponseEntity<ExternalChatResponse> externalResponse = restTemplate.postForEntity(
                                        EXTERNAL_CHAT_API_URL, externalRequest, ExternalChatResponse.class);

                        ExternalChatResponse body = externalResponse.getBody();
 
                        if (body == null) {
                                return ResponseEntity.internalServerError().build();
                        }

                        ChatbotResponse response = new ChatbotResponse();
                        response.setText(body.getMessage());

                        // Mapping documentLinks = sources array inside object file key
                        List<String> documentLinks = new ArrayList<>();
                        if (body.getSources() != null) {
                                // Fallback to top-level sources
                                documentLinks = body.getSources().stream().map(a -> a.get("file")).toList();
                        }
                        response.setDocumentLinks(documentLinks);

                        // Mapping buttons = options key is string of array
                        List<ChatbotButton> buttons = new ArrayList<>();
                        if (body.getOptions() != null) {
                                buttons = body.getOptions().stream()
                                                .map(opt -> new ChatbotButton(opt, opt, opt))
                                                .collect(Collectors.toList());
                        }

                        response.setButtons(buttons);
                        response.setImageLinks(new ArrayList<>()); // Images not explicitly mapped from external API

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Error calling external chatbot API", e);
                        return ResponseEntity.internalServerError().build();
                }
        }
}
