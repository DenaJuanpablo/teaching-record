package com.web.dto;

import java.util.List;

public class TranscriptView {
    public List<Segment> segments;

    public TranscriptView() {}

    public TranscriptView(List<Segment> segments) {
        this.segments = segments;
    }
}