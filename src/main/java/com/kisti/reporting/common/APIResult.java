package com.kisti.reporting.common;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class APIResult {
    private Object result;
    private boolean success;
    @Singular("errors")
    private List<APIError> errors;
    @Singular("messages")
    private List<APIMessage> messages;
}
