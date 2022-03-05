package com.orangeandbronze.enlistment.controllers;

import com.orangeandbronze.enlistment.domain.Admin;
import com.orangeandbronze.enlistment.domain.AdminRepository;
import com.orangeandbronze.enlistment.domain.Days;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.*;

import java.time.LocalTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.orangeandbronze.enlistment.domain.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
@SpringBootTest
class SectionsControllerIT extends AbstractControllerIT {

    @Autowired
    private AdminRepository adminRepository;

    private void initTempDatabase() {
        jdbcTemplate.update("INSERT INTO admin (id, firstname, lastname) VALUES (?,?,?)",
                DEFAULT_ADMIN_ID, "firstname", "lastname");
        jdbcTemplate.update("INSERT INTO room (name, capacity) VALUES (?, ?)", DEFAULT_ROOM_NAME, DEFAULT_ROOM_CAPACITY);
        jdbcTemplate.update("INSERT INTO subject (subject_id) VALUES (?)", DEFAULT_SUBJECT.toString());
        jdbcTemplate.update("INSERT INTO faculty (faculty_number, firstname, lastname) VALUES (?,?,?)",
                DEFAULT_FACULTY_NUMBER, "firstname", "lastname");
    }

    @Test
    void createSection_save_to_db() throws Exception {
        // Given parameters sectionId, subjectId, days, start time, end time, roomName,
        // to create a new section, and in the DB: an admin, a room & a subject
        initTempDatabase();
        // When the POST method on path "/sections" is invoked, with
            // the parameters for creating a new section, and
            // the roomName and subjectId parameters matching the room and subject records in the db
            // with an admin object in session in order to create sections
        Admin admin = adminRepository.findById(DEFAULT_ADMIN_ID).orElseThrow(() ->
                new NoSuchElementException("No admin w/ id " + DEFAULT_ADMIN_ID + " found in DB."));
        mockMvc.perform(post("/sections").
                sessionAttr("admin", admin).
                param("sectionId", DEFAULT_SECTION_ID).
                param("subjectId", DEFAULT_SUBJECT.toString()).
                param("days", String.valueOf(Days.MTH)).
                param("start", "15:30").
                param("end", "16:00").
                param("roomName", DEFAULT_ROOM_NAME).
                param("facultyNumber", String.valueOf(DEFAULT_FACULTY_NUMBER))
        );
        // Then a new record is created in the section table together with the correct column/field values
        Map<String, Object> results = jdbcTemplate.queryForMap("SELECT * FROM section WHERE section_id = ?", DEFAULT_SECTION_ID);
        assertAll(
                () -> assertEquals(DEFAULT_SECTION_ID, results.get("section_id")),
                () -> assertEquals(DEFAULT_SUBJECT.toString(), results.get("subject_subject_id")),
                () -> assertEquals(Days.MTH.ordinal(), results.get("days")),
                () -> assertEquals(LocalTime.parse("15:30"), LocalTime.parse(results.get("start_time").toString())),
                () -> assertEquals(LocalTime.parse("16:00"), LocalTime.parse(results.get("end_time").toString())),
                () -> assertEquals(DEFAULT_ROOM_NAME, results.get("room_name")),
                () -> assertEquals(DEFAULT_FACULTY_NUMBER, results.get("instructor_faculty_number"))
        );
    }

    @Test
    void createSection_create_section_with_section_id_already_exists_in_db() throws Exception {
        // Given parameters sectionId, subjectId, days, start time, end time, roomName,
        // to create a new section, and in the DB: an admin, a room & a subject,
        // and a section with the given sectionId already exists in the database
        initTempDatabase();
        jdbcTemplate.update("INSERT INTO section (section_id, number_of_students, days, start_time, end_time, room_name, subject_subject_id, instructor_faculty_number) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                DEFAULT_SECTION_ID, 0, Days.MTH.ordinal(), LocalTime.of(9, 0), LocalTime.of(10, 0), DEFAULT_ROOM_NAME, DEFAULT_SUBJECT.toString(),
                DEFAULT_FACULTY_NUMBER);
        // When the POST method on path "/sections" is invoked, with
            // the parameters for creating a new section, and
            // the roomName and subjectId parameters matching the room and subject records in the db
            // with an admin object in session in order to create sections
        // Then an exception should be thrown indicating that a new section cannot be created with a
        // duplicate section id, and no duplicate record shall be inserted to the section table of the db
        Admin admin = adminRepository.findById(DEFAULT_ADMIN_ID).orElseThrow(() ->
                new NoSuchElementException("No admin w/ id " + DEFAULT_ADMIN_ID + " found in DB."));
        mockMvc.perform(post("/sections").
                sessionAttr("admin", admin).
                param("sectionId", DEFAULT_SECTION_ID).
                param("subjectId", DEFAULT_SUBJECT.toString()).
                param("days", String.valueOf(Days.MTH)).
                param("start", "15:30").
                param("end", "16:00").
                param("roomName", DEFAULT_ROOM_NAME).
                param("facultyNumber", String.valueOf(DEFAULT_FACULTY_NUMBER))
        );
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM section WHERE section_id = ?",
                Integer.class, DEFAULT_SECTION_ID
        );
        assertEquals(1, count);
    }

    @Test
    void createSection_create_section_with_overlapping_schedule_and_same_instructor() throws Exception {
        // Given parameters sectionId, subjectId, days, start time, end time, roomName,
        // to create a new section, and in the DB: an admin, a room & a subject,
        // and a section with chosen schedule and instructor
        initTempDatabase();
        jdbcTemplate.update("INSERT INTO section (section_id, number_of_students, days, start_time, end_time, room_name, subject_subject_id, instructor_faculty_number) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                DEFAULT_SECTION_ID, 0, Days.MTH.ordinal(), LocalTime.of(9, 0), LocalTime.of(10, 0), DEFAULT_ROOM_NAME, DEFAULT_SUBJECT.toString(),
                DEFAULT_FACULTY_NUMBER);
        // When the POST method on path "/sections" is invoked, with
            // the parameters for creating a new section wherein the schedule will overlap with the existing section in the db's schedule, and
            // the same instructor (faculty number) was chosen
            // with an admin object in session in order to create sections
        // Then an exception should be thrown indicating that a new section cannot be created with an overlapping schedule and same instructor
        // and no new section record shall be inserted to the db
        Admin admin = adminRepository.findById(DEFAULT_ADMIN_ID).orElseThrow(() ->
                new NoSuchElementException("No admin w/ id " + DEFAULT_ADMIN_ID + " found in DB."));
        mockMvc.perform(post("/sections").
                sessionAttr("admin", admin).
                param("sectionId", "X12").
                param("subjectId", DEFAULT_SUBJECT.toString()).
                param("days", String.valueOf(Days.MTH)).
                param("start", "09:30").
                param("end", "10:30").
                param("roomName", DEFAULT_ROOM_NAME).
                param("facultyNumber", String.valueOf(DEFAULT_FACULTY_NUMBER))
        );
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM section WHERE section_id = ?", Integer.class, "X12");
        assertEquals(0, count);
    }

}
