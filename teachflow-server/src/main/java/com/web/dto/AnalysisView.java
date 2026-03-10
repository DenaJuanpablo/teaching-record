package com.web.dto;

import java.util.List;

public class AnalysisView {
    public String summary;
    public List<String> keywords;
    public List<Object> outline;

    public AnalysisView() {}

    public AnalysisView(String summary, List<String> keywords, List<Object> outline) {
        this.summary = summary;
        this.keywords = keywords;
        this.outline = outline;
    }




}