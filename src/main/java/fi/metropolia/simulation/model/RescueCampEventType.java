package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.IEventType;

/**
 * MODEL: Event types enumeration for the rescue camp simulation
 * Defines all possible events in the rescue camp system
 */
public enum RescueCampEventType implements IEventType {
    // Arrival events
    SURVIVOR_ARRIVAL,

    // Gate processing events
    GATE_PROCESSING_COMPLETE,

    // Medical treatment events
    MEDICAL_TREATMENT_COMPLETE,

    // Registration events
    REGISTRATION_COMPLETE,

    // Service events
    COMMUNICATION_SERVICE_COMPLETE,
    SUPPLIES_DISTRIBUTION_COMPLETE,

    // Shelter assignment events
    CHILD_SHELTER_ASSIGNMENT_COMPLETE,
    ADULT_SHELTER_ASSIGNMENT_COMPLETE,
    FAMILY_SHELTER_ASSIGNMENT_COMPLETE
}