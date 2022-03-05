package com.orangeandbronze.enlistment.controllers;

import com.orangeandbronze.enlistment.domain.Days;
import com.orangeandbronze.enlistment.domain.Student;
import com.orangeandbronze.enlistment.domain.StudentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

import static com.orangeandbronze.enlistment.controllers.UserAction.*;
import static com.orangeandbronze.enlistment.domain.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// @Testcontainers for test containers and usage of Docker, and @DirtiesContext to dump / reset changes per method executed
// to avoid interference with other test methods such as database manipulation (primary key already exists, etc.)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
@AutoConfigureMockMvc
class EnlistControllerIT extends AbstractControllerIT {

    @Autowired
    private StudentRepository studentRepository;

    private void initTempDatabase() {
        jdbcTemplate.update("INSERT INTO student (student_number, firstname, lastname) VALUES (?,?,?)",
                DEFAULT_STUDENT_NUMBER, "firstname", "lastname");
        jdbcTemplate.update("INSERT INTO room (name, capacity) VALUES (?, ?)", DEFAULT_ROOM_NAME, DEFAULT_ROOM_CAPACITY);
        jdbcTemplate.update("INSERT INTO subject (subject_id) VALUES (?)", DEFAULT_SUBJECT.toString());
        jdbcTemplate.update("INSERT INTO section (section_id, number_of_students, days, start_time, end_time, room_name, subject_subject_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                DEFAULT_SECTION_ID, 0, Days.MTH.ordinal(), LocalTime.of(9, 0), LocalTime.of(10, 0), DEFAULT_ROOM_NAME, DEFAULT_SUBJECT.toString());
    }

    // NOTE: throwing exception is perfectly fine for test code but not for production code
    @Test
    void enlistOrCancel_enlist_student_in_section() throws Exception {
        // Given in the DB: a student & a section
        initTempDatabase();
        // When the POST method on path "/enlist" is invoked, with
            // parameters for sectionId matching the record in the db, and UserAction "ENLIST"
            // with a student object in session corresponding to the student record in the db
        Student student = studentRepository.findById(DEFAULT_STUDENT_NUMBER).orElseThrow(() ->
                new NoSuchElementException("No student w/ student num " + DEFAULT_STUDENT_NUMBER + " found in DB."));
        mockMvc.perform(post("/enlist").sessionAttr("student", student).param("sectionId", DEFAULT_SECTION_ID)
                .param("userAction", ENLIST.name()));
        // Then a new record in the student_sections, containing the corresponding studentNumber and sectionId
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_sections WHERE student_student_number = ? AND sections_section_id = ?",
                Integer.class, DEFAULT_STUDENT_NUMBER, DEFAULT_SECTION_ID
        );
        assertEquals(1, count);
    }

    @Test
    void enlistOrCancel_cancel_student_in_section() throws Exception {
        // Given in the DB: a student & a section, and an enlistment record existing in student_sections
        initTempDatabase();
        jdbcTemplate.update("INSERT INTO student_sections (student_student_number, sections_section_id) "
                + "VALUES (?, ?)", DEFAULT_STUDENT_NUMBER, DEFAULT_SECTION_ID);
        // When the POST method on path "/enlist" is invoked, with
            // parameters for sectionId matching the record in the db, and UserAction "ENLIST"
            // with a student object in session corresponding to the student record in the db
        Student student = studentRepository.findById(DEFAULT_STUDENT_NUMBER).orElseThrow(() ->
                new NoSuchElementException("No student w/ student num " + DEFAULT_STUDENT_NUMBER + " found in DB."));
        mockMvc.perform(post("/enlist").sessionAttr("student", student).param("sectionId", DEFAULT_SECTION_ID)
                .param("userAction", CANCEL.name()));
        // Then a record containing the corresponding studentNumber and sectionId in the student_sections should be deleted
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_sections WHERE student_student_number = ? AND sections_section_id = ?",
                Integer.class, DEFAULT_STUDENT_NUMBER, DEFAULT_SECTION_ID
        );
        assertEquals(0, count);
    }

    private final static int FIRST_STUDENT_ID = 11;
    private final static int NUMBER_OF_STUDENTS = 20;
    private final static int LAST_STUDENT_NUMBER = FIRST_STUDENT_ID + NUMBER_OF_STUDENTS - 1;

    private void initTempDatabaseConcurrency(int capacity) {
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = FIRST_STUDENT_ID; i <= LAST_STUDENT_NUMBER; i++) {
            batchArgs.add(new Object[]{i, "firstname", "lastname"});
        }
        jdbcTemplate.batchUpdate("INSERT INTO student(student_number, firstname, lastname) VALUES (?, ?, ?)", batchArgs);
        jdbcTemplate.update("INSERT INTO room(name, capacity) VALUES (?, ?)", DEFAULT_ROOM_NAME, capacity);
        jdbcTemplate.update("INSERT INTO subject(subject_id) VALUES (?)", DEFAULT_SUBJECT.toString());
        jdbcTemplate.update(
                "INSERT INTO section(section_id, number_of_students, days, start_time, end_time, room_name, subject_subject_id)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)",
                DEFAULT_SECTION_ID, 0, Days.MTH.ordinal(), LocalTime.of(9, 0), LocalTime.of(10, 0), DEFAULT_ROOM_NAME, DEFAULT_SUBJECT.toString()
        );
    }

    @Test
    void enlistOrCancel_enlist_concurrently_same_section_enough_capacity() throws Exception {
        // Given several students & section has enough capacity to accommodate all
        initTempDatabaseConcurrency(NUMBER_OF_STUDENTS);
        // When the students enlist concurrently
        startEnlistmentThreads();
        // Then all students will be able to enlist successfully
        int numStudents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_sections where sections_section_id = '" +
                        DEFAULT_SECTION_ID + "'", Integer.class
        );
        assertEquals(NUMBER_OF_STUDENTS, numStudents);
    }

    @Test
    void enlistOrCancel_enlist_concurrent_separate_section_instances_representing_same_record_students_beyond_capacity() throws Exception {
        // Given multiple section objects representing same record & several students; section capacity 1
        initTempDatabaseConcurrency(1);
        // When each student enlists in separate section instances
        startEnlistmentThreads();
        // Then only one student should be able to enlist successfully
        int numStudents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_sections where sections_section_id = '" +
                        DEFAULT_SECTION_ID + "'", Integer.class
        );
        assertEquals(1, numStudents);
    }

    private void startEnlistmentThreads() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = FIRST_STUDENT_ID; i <= LAST_STUDENT_NUMBER; i++) {
            final int studentNo = i;
            new EnlistmentThread(studentRepository.findById(studentNo).orElseThrow(() ->
                    new NoSuchElementException("No student w/ student num " + studentNo + " found in DB.")),
                    latch, mockMvc).start();
        }
        latch.countDown();
        Thread.sleep(5000); // wait time to allow all the threads to finish
    }

    private static class EnlistmentThread extends Thread {
        private final Student student;
        private final CountDownLatch latch;
        private final MockMvc mockMvc;

        public EnlistmentThread(Student student, CountDownLatch latch, MockMvc mockMvc) {
            this.student = student;
            this.latch = latch;
            this.mockMvc = mockMvc;
        }

        @Override
        public void run() {
            try {
                latch.await(); // The thread keeps waiting till it is informed
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                mockMvc.perform(post("/enlist").sessionAttr("student", student)
                        .param("sectionId", DEFAULT_SECTION_ID).param("userAction", ENLIST.name()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
