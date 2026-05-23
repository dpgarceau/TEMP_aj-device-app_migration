package com.aerojudge.judge.dto;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "figures")
public class FiguresUploadDTO {
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<FigureUploadDTO> figure;

    public FiguresUploadDTO(List<FigureUploadDTO> figure) {
        this.figure = figure;
    }

    public List<FigureUploadDTO> getFigure() {
        return figure;
    }

    public void setFigure(List<FigureUploadDTO> figure) {
        this.figure = figure;
    }
}
