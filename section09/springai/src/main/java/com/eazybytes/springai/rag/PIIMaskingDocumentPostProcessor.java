package com.eazybytes.springai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Masks sensitive information (e.g., emails, phone numbers) in document content
 * to ensure privacy and compliance. Uses regex patterns to identify and redact PII.
 */
public class PIIMaskingDocumentPostProcessor implements DocumentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PIIMaskingDocumentPostProcessor.class);

    // Regex patterns for common PII
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b",
            Pattern.CASE_INSENSITIVE);

    private static final String EMAIL_REPLACEMENT = "[REDACTED_EMAIL]";
    private static final String PHONE_REPLACEMENT = "[REDACTED_PHONE]";

    private PIIMaskingDocumentPostProcessor() {
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");
        Assert.noNullElements(documents, "documents cannot contain null elements");

        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        logger.debug("Masking sensitive information in documents for query: {}", query.text());

        return documents.stream()
                .map(document -> {
                    String text = document.getText() != null ? document.getText() : "";
                    // Apply PII masking
                    String maskedText = maskSensitiveInformation(text);
                    return document.mutate()
                            .text(maskedText)
                            .metadata("pii_masked", true)
                            .build();
                })
                .toList();
    }

    private String maskSensitiveInformation(String text) {
        String masked = text;
        // Mask emails
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT);
        // Mask phone numbers
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT);
        return masked;
    }

    public static PIIMaskingDocumentPostProcessor builder() {
        return new PIIMaskingDocumentPostProcessor();
    }
}
