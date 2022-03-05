package com.orangeandbronze.enlistment.domain;

import javax.persistence.*;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notBlank;


@Entity
public class Faculty {
    @Id
    private final int facultyNumber;
    @Column(
            columnDefinition = "VARCHAR(255)"
    )
    private final String firstname;
    @Column(
            columnDefinition = "VARCHAR(255)"
    )
    private final String lastname;

    Faculty(int facultyNumber, String firstname, String lastname) {
        isTrue(facultyNumber >= 0, "facultyNumber must be non-negative, was: " + facultyNumber);
        notBlank(firstname);
        notBlank(lastname);
        this.facultyNumber = facultyNumber;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public int getFacultyNumber() {
        return this.facultyNumber;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    @Override
    public String toString() {
        return facultyNumber + "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faculty faculty = (Faculty) o;
        return facultyNumber == faculty.facultyNumber;
    }

    @Override
    public int hashCode() {
        return facultyNumber;
    }

    // For JPA only. Do not call!
    private Faculty () {
        facultyNumber = -1;
        firstname = null;
        lastname = null;
    }
}
