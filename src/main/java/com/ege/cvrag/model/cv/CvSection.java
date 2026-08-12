package com.ege.cvrag.model.cv;

/** A Markdown section of the CV: its heading and full body text (heading included). */
public record CvSection(String heading, String body) {}
