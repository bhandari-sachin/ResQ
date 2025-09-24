package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.IEventType;

/**
 * MODEL: Event types enumeration for the rescue camp simulation
 * Defines all possible events in the rescue camp system
 */
public enum RescueCampEventType implements IEventType {
    // Arrival events
    SURVIVOR_ARRIVAL,

    // Gate processing events (kept as-is if used elsewhere)
    GATE_PROCESSING_COMPLETE,

    // Medical treatment events
    MEDICAL_TREATMENT_COMPLETE,          // SC-4

    // Registration events
    REGISTRATION_COMPLETE,               // SC-1

    // Service events
    COMMUNICATION_SERVICE_COMPLETE,      // SC-2
    SUPPLIES_DISTRIBUTION_COMPLETE,      // SC-3

    // NEW: Accommodation center
    ACCOMMODATION_CENTER_COMPLETE,       // SC-5

    // Shelter assignment events
    CHILD_SHELTER_ASSIGNMENT_COMPLETE,   // SC-6
    ADULT_SHELTER_ASSIGNMENT_COMPLETE    // SC-7
}
