package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Samy on 10/11/2017.
 */

public class ReservationResponse extends APIResponse {
    protected Integer reservation_id;
    protected String seance;

    public Integer getReservation_id() { return this.reservation_id; }
    public String getSeance() { return this.seance; }
}
