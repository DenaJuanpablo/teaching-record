package com.web.dto;

public class Segment {
    public Long startMs;
    public Long endMs;
    public String text;
    public String speaker;

    public Segment() {}

    public Segment(Long startMs, Long endMs, String text, String speaker) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
        this.speaker = speaker;
    }
}