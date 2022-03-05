package com.orangeandbronze.enlistment.controllers;

import com.orangeandbronze.enlistment.domain.*;
import org.hibernate.Session;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import java.util.Optional;

import static com.orangeandbronze.enlistment.domain.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EnlistControllerTest {

    @Test
    void enlistOrCancel_enlist_student_in_section() {
        // Given the controller w/ a student in session, param of sectionId to enlist, & UserAction "ENLIST"
        Student student = mock(Student.class);
        String sectionId = DEFAULT_SECTION_ID;
        UserAction userAction = UserAction.ENLIST;
        // When enlist (post) method is called
        SectionRepository sectionRepository = mock(SectionRepository.class);
        Section section = new Section(sectionId, DEFAULT_SUBJECT,
                MTH830to10, DEFAULT_ROOM, DEFAULT_FACULTY);
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        StudentRepository studentRepository = mock(StudentRepository.class);
        EnlistController controller = new EnlistController();
        controller.setSectionRepo(sectionRepository);
        controller.setStudentRepo(studentRepository);
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        controller.setEntityManager(entityManager); // to avoid NullPointerException
        String returnVal = controller.enlistOrCancel(student, sectionId, userAction);
        // Then
        // - retrieve the Section object from the DB using the sectionId
        verify(sectionRepository).findById(sectionId);
        // - student.enlist method will be called, passing in the section
        verify(student).enlist(section);
        // - save student to DB
        verify(studentRepository).save(student);
        // - save section to DB
        verify(sectionRepository).save(section);
        assertEquals("redirect:enlist", returnVal);
    }

}
