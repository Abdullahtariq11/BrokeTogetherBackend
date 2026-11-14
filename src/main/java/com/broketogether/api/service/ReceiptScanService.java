package com.broketogether.api.service;

import com.broketogether.api.dto.ReceiptScanResponse;
import com.broketogether.api.model.User;
import com.broketogether.api.utility.Utility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptScanService extends Utility {

    private static final Logger log = LoggerFactory.getLogger(ReceiptScanService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api-key}")
    private String openAiApiKey;

    private final RestClient restClient = RestClient.create();

    public ReceiptScanResponse scan(MultipartFile file) {
        try {
            User caller = getUserDetails();
            if (!Boolean.TRUE.equals(caller.getPremium())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Receipt scanning is a Premium feature");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mediaType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            Map<String, Object> body = Map.of(
                "model", "gpt-4o",
                "max_tokens", 300,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of(
                            "type", "image_url",
                            "image_url", Map.of(
                                "url", "data:" + mediaType + ";base64," + base64Image,
                                "detail", "low"
                            )
                        ),
                        Map.of(
                            "type", "text",
                            "text", """
                                Look at this receipt image. Extract the final total amount paid (after tax and tips).
                                Reply in this exact JSON format and nothing else:
                                {"amount": 12.34, "note": "Store name or any helpful context"}
                                If you cannot determine the total, reply: {"amount": null, "note": "Could not parse receipt"}
                                Currency is CAD. Return numbers only, no currency symbols.
                                """
                        )
                    )
                ))
            );

            String responseBody = restClient.post()
                .uri(OPENAI_CHAT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

            JsonNode root = mapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Strip markdown code fences if present
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode result = mapper.readTree(content);
            JsonNode amountNode = result.path("amount");
            String note = result.path("note").asText("Receipt scanned");

            BigDecimal amount = amountNode.isNull() ? null : new BigDecimal(amountNode.asText());
            return new ReceiptScanResponse(amount, note);

        } catch (Exception e) {
            log.error("Receipt scan failed", e);
            return new ReceiptScanResponse(null, "Could not process receipt. Please enter amount manually.");
        }
    }
}
