package com.example.bankapp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Writes financial events to the dedicated {@code audit} logger as single-line JSON so the
 * stream can be shipped to a WORM store and queried by auditors (ITGC-LOG-11).
 */
@Component
public class AuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger("audit");

    public void log(AuditEvent event) {
        LOG.info(render(event));
    }

    private String render(AuditEvent event) {
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, Object> field : event.fields().entrySet()) {
            if (field.getValue() == null) {
                continue;
            }
            appendField(json, field.getKey(), String.valueOf(field.getValue()));
        }
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        if (correlationId != null) {
            appendField(json, CorrelationIdFilter.CORRELATION_ID_KEY, correlationId);
        }
        return json.append("}").toString();
    }

    private void appendField(StringBuilder json, String name, String value) {
        if (json.length() > 1) {
            json.append(",");
        }
        json.append("\"").append(escape(name)).append("\":\"").append(escape(value)).append("\"");
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                escaped.append("\\\"");
            } else if (c == '\\') {
                escaped.append("\\\\");
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c == '\r') {
                escaped.append("\\r");
            } else if (c == '\t') {
                escaped.append("\\t");
            } else if (c < 0x20) {
                escaped.append(String.format("\\u%04x", (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
