package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.IEventType;

/**
 * Event types are defined by the requirements of the simulation model
 * <p>
 * TODO: This must be adapted to the actual simulator
 */
public enum EventType implements IEventType {
    ARR1, DEP1, DEP2, DEP3;
}
