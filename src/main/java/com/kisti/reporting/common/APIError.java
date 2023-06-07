package com.kisti.reporting.common;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class APIError {
    private String message;
    @Singular("data")
    private Map<String, Object> data;
}
