package com.rocketseat.planner.trip;

import com.rocketseat.planner.activity.ActivityData;
import com.rocketseat.planner.activity.ActivityRequestPaylod;
import com.rocketseat.planner.activity.ActivityResponse;
import com.rocketseat.planner.activity.ActivityService;
import com.rocketseat.planner.link.LinkData;
import com.rocketseat.planner.link.LinkRequestPayload;
import com.rocketseat.planner.link.LinkResponse;
import com.rocketseat.planner.link.LinkService;
import com.rocketseat.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController()
@RequestMapping("/trips")
public class TripController {
    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TripRepository repository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    // region TRIP
    @PostMapping()
    public ResponseEntity<TripCreateResponse> createTrip (@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);

        this.repository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails (@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip newTrip = trip.get();
            newTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            newTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            newTrip.setDestination(payload.destination());

            this.repository.save(newTrip);

            return ResponseEntity.ok(newTrip);
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/confirm")
    public  ResponseEntity<Trip> confirmTrip(@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);

            this.repository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(id);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }
    // endregion

    // region PARTICIPANT
    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant (@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            if(rawTrip.getIsConfirmed()) this.participantService.triggerConfirmationEmailToParticipant(payload.email());

            return ResponseEntity.ok(participantResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipants(@PathVariable UUID id) {
        List<ParticipantData> participantList = this.participantService.getAllParticipantsFromEvent(id);

        return ResponseEntity.ok(participantList);
    }
    // endregion

    // region ACTIVITIES
    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id) {
        List<ActivityData> activiesList = this.activityService.getAllActivesFromId(id);

        return ResponseEntity.ok(activiesList);
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity (@PathVariable UUID id, @RequestBody ActivityRequestPaylod payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            ActivityResponse activityResponse = this.activityService.registerActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);
        }

        return ResponseEntity.notFound().build();
    }
    // endregion

    // region LINK
    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink (@PathVariable UUID id, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            LinkResponse linkResponse = this.linkService.registerLink(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);
        }

        return ResponseEntity.notFound().build();
    }
    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinkes(@PathVariable UUID id) {
        List<LinkData> linksList = this.linkService.getAllLinksFromTrip(id);

        return ResponseEntity.ok(linksList);
    }
    // enregion

}
