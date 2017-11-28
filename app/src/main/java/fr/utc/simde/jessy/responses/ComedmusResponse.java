package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComedmusResponse extends APIResponse {
    protected Integer reservationId;
    protected String seance;

    public Integer getReservationId() { return this.reservationId; }
    public String getSeance() { return this.seance; }
}
