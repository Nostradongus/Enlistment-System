package com.orangeandbronze.enlistment.domain;

import javax.persistence.*;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.Validate.*;

@Entity
public class Room {
    @Id
    private final String name;
    private final int capacity;
    @OneToMany
    private final Collection<Section> sections = new HashSet<>();

    Room(String name, int capacity, Collection<Section> sections) {
        notBlank(name);
        isTrue(isAlphanumeric(name), "roomName must be alphanumeric, was: " + name);
        isTrue(capacity > 0, "capacity must be greater than zero, was: " + capacity);
        notNull(sections);
        this.name = name;
        this.capacity = capacity;
        this.sections.addAll(sections);
        this.sections.removeIf(Objects::isNull);
    }

    public Room(String roomName, int capacity) {
        this(roomName, capacity, Collections.emptyList());
    }

    void checkIfAtOrOverCapacity(int occupancy) {
        isTrue(occupancy >= 0, "occupancy must be non-negative, was: " + occupancy);
        if (occupancy >= capacity) {
            throw new CapacityException("at or over capacity of " + capacity + "  at occupancy of " + occupancy);
        }
    }

    void addSection(Section newSection) {
        notNull(newSection);
        sections.forEach(currSection -> currSection.checkForScheduleConflict(newSection));
        sections.add(newSection);
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        return name != null ? name.equals(room.name) : room.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    // For JPA only! Do not call!
    private Room() {
        name = null;
        capacity = -1;
    }
}
