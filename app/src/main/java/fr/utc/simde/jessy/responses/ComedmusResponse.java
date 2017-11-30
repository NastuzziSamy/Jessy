package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComedmusResponse extends APIResponse {
    protected Integer reservation_id;
    protected String seance;

    public Integer getReservationId() { return this.reservation_id; }
    public String getSeance() { return this.seance; }
}
