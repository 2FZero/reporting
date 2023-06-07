package com.kisti.reporting.common;

import lombok.Data;

import java.util.HashMap;

@Data
public class Report {
    String reportPath;
    HashMap<String, Object> reportParam;
    HashMap<String, Object> printInfo;
}
