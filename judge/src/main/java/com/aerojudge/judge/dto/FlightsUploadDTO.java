package com.aerojudge.judge.dto;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "flights")
public class FlightsUploadDTO {
    public int flightline = 1;
    @JacksonXmlElementWrapper(useWrapping = false)
	public List<FlightUploadDTO> flight;
    
    public FlightsUploadDTO(List<FlightUploadDTO> flight, int line_number ) {
        this.flight = flight; this.flightline = line_number;
    }
    public int getFlightline() {
        return flightline;
    }
    public void setFlightline(int flightline) {
        this.flightline = flightline;
    }
    public List<FlightUploadDTO> getFlight() {
        return flight;
    }
    public void setFlight(List<FlightUploadDTO> flight) {
        this.flight = flight;
    }
    

    
}
