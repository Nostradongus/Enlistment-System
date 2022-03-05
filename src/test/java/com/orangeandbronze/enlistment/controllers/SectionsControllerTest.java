package com.orangeandbronze.enlistment.controllers;

import com.orangeandbronze.enlistment.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.orangeandbronze.enlistment.domain.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectionsControllerTest {

    @Test
    void createSection_save_new_section_to_repository() {
        // Given a controller w/ params sectionId, subjectId, days, start time, end time, roomName, facultyNumber
        // and redirectAttrs to create a new section.
        String sectionId = DEFAULT_SECTION_ID;
        String subjectId = DEFAULT_SUBJECT.toString();
        Days days = Days.WS;
        String startTime = "11:30";
        String endTime = "12:00";
        String roomName = DEFAULT_ROOM_NAME;
        String facultyNumber = String.valueOf(DEFAULT_FACULTY_NUMBER);
        // When create section (post) method is called
        RedirectAttributes redirectAttrs = mock(RedirectAttributes.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        Subject subject = mock(Subject.class);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        AdminRepository adminRepository = mock(AdminRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        Room room = mock(Room.class);
        when(roomRepository.findById(roomName)).thenReturn(Optional.of(room));
        SectionRepository sectionRepository = mock(SectionRepository.class);
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        when(facultyRepository.findById(DEFAULT_FACULTY_NUMBER)).thenReturn(Optional.of(DEFAULT_FACULTY));
        SectionsController controller = new SectionsController();
        controller.setSubjectRepo(subjectRepository);
        controller.setAdminRepo(adminRepository);
        controller.setRoomRepo(roomRepository);
        controller.setSectionRepo(sectionRepository);
        controller.setFacultyRepository(facultyRepository);
        String returnVal = controller.createSection(sectionId, subjectId, days, startTime, endTime, roomName, facultyNumber, redirectAttrs);
        // Then a new section shall be created and saved to the DB
        // Then
            // - it should first get the subject and room from the DB
            // - create a new section with the given parameters
            // - and save the new section in the DB afterwards
        Period period = new Period(LocalTime.parse(startTime), LocalTime.parse(endTime));
        Schedule schedule = new Schedule(days, period);
        Section section = new Section(sectionId, subject, schedule, room, DEFAULT_FACULTY);
        assertAll(
                () -> verify(subjectRepository).findById(subjectId),
                () -> verify(roomRepository).findById(roomName),
                () -> verify(sectionRepository).save(section),
                () -> assertEquals("redirect:sections", returnVal)
        );
    }

    @Test
    void createSection_create_section_with_blank_section_id() {
        // Given a controller w/ params sectionId that is blank, subjectId, days, start time, end time, roomName, facultyNumber
        // and redirectAttrs to create a new section.
        String sectionId = "";
        String subjectId = DEFAULT_SUBJECT.toString();
        Days days = Days.WS;
        String startTime = "11:30";
        String endTime = "12:00";
        String roomName = DEFAULT_ROOM_NAME;
        String facultyNumber = String.valueOf(DEFAULT_FACULTY_NUMBER);
        // When create section (post) method is called
        RedirectAttributes redirectAttrs = mock(RedirectAttributes.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        Subject subject = mock(Subject.class);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        AdminRepository adminRepository = mock(AdminRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        Room room = mock(Room.class);
        when(roomRepository.findById(roomName)).thenReturn(Optional.of(room));
        SectionRepository sectionRepository = mock(SectionRepository.class);
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        when(facultyRepository.findById(DEFAULT_FACULTY_NUMBER)).thenReturn(Optional.of(DEFAULT_FACULTY));
        SectionsController controller = new SectionsController();
        controller.setSubjectRepo(subjectRepository);
        controller.setAdminRepo(adminRepository);
        controller.setRoomRepo(roomRepository);
        controller.setSectionRepo(sectionRepository);
        controller.setFacultyRepository(facultyRepository);
        // Then an exception should be thrown indicating that the sectionId must not be blank or empty
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.createSection(sectionId, subjectId, days, startTime, endTime, roomName, facultyNumber, redirectAttrs)
        );
    }

    @Test
    void createSection_create_section_with_overlapping_schedule_and_instructor() {
        // Given a controller w/ params sectionId, subjectId, days, start time, end time, roomName, facultyNumber
        // and redirectAttrs to create a new section, and an existing section in the repository whose schedule will overlap
        // with the new section being created and has the same instructor as well.
        String sectionId = "X12";
        String subjectId = DEFAULT_SUBJECT.toString();
        Days days = Days.WS;
        String startTime = "11:30";
        String endTime = "12:00";
        String roomName = DEFAULT_ROOM_NAME;
        String facultyNumber = String.valueOf(DEFAULT_FACULTY_NUMBER);
        RedirectAttributes redirectAttrs = mock(RedirectAttributes.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        Subject subject = mock(Subject.class);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        AdminRepository adminRepository = mock(AdminRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        Room room = mock(Room.class);
        when(roomRepository.findById(roomName)).thenReturn(Optional.of(room));
        SectionRepository sectionRepository = mock(SectionRepository.class);
        Schedule schedule = new Schedule(Days.WS, new Period(LocalTime.parse("09:00"), LocalTime.parse("12:00")));
        Section section = new Section(DEFAULT_SECTION_ID, DEFAULT_SUBJECT, schedule, DEFAULT_ROOM, DEFAULT_FACULTY);
        List<Section> sections = new ArrayList<>();
        sections.add(section);
        when(sectionRepository.findAll()).thenReturn(sections);
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        when(facultyRepository.findById(DEFAULT_FACULTY_NUMBER)).thenReturn(Optional.of(DEFAULT_FACULTY));
        SectionsController controller = new SectionsController();
        controller.setSubjectRepo(subjectRepository);
        controller.setAdminRepo(adminRepository);
        controller.setRoomRepo(roomRepository);
        controller.setSectionRepo(sectionRepository);
        controller.setFacultyRepository(facultyRepository);
        // When create section (post) method is called
        // Then an exception should be thrown indicating that a new section cannot be created given that there is already
        // an existing section whose schedule overlaps with the new section's schedule and has the same instructor as well
        assertThrows(
                EnlistmentException.class,
                () -> controller.createSection(sectionId, subjectId, days, startTime, endTime, roomName, facultyNumber, redirectAttrs)
        );
    }

}
