package com.harness.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String text;
    private List<String> imageLinks;
    private List<String> documentLinks;
    private List<ChatbotButton> buttons;
}
