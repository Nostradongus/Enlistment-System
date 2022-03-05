package com.orangeandbronze.enlistment.controllers;

import com.orangeandbronze.enlistment.domain.*;
import com.orangeandbronze.enlistment.domain.Period;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.*;

import java.time.*;
import java.util.*;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("sections")
@SessionAttributes("admin")
class SectionsController {

    @Autowired
    private SubjectRepository subjectRepo;
    @Autowired
    private AdminRepository adminRepo;
    @Autowired
    private RoomRepository roomRepo;
    @Autowired
    private SectionRepository sectionRepo;
    @Autowired
    private FacultyRepository facultyRepository;

    @ModelAttribute("admin")
    public Admin admin(Integer id) {
        return adminRepo.findById(id).orElseThrow(() -> new NoSuchElementException("no admin found for adminId " + id));
    }

    @GetMapping
    public String showPage(Model model, Integer id) {
        Admin admin = id == null ? (Admin) model.getAttribute("admin") :
                adminRepo.findById(id).orElseThrow(() -> new NoSuchElementException("no admin found for adminId " + id));
        model.addAttribute("admin", admin);
        model.addAttribute("subjects", subjectRepo.findAll());
        model.addAttribute("rooms", roomRepo.findAll());
        model.addAttribute("sections", sectionRepo.findAll());
        model.addAttribute("instructors", facultyRepository.findAll());
        return "sections";
    }

    @PostMapping
    public String createSection(@RequestParam String sectionId, @RequestParam(required = false) String subjectId, @RequestParam Days days,
                                @RequestParam String start, @RequestParam String end, @RequestParam(required = false) String roomName,
                                @RequestParam(required = false) String facultyNumber, RedirectAttributes redirectAttrs) {
        notBlank(subjectId, "Please choose a subject for the new section!");
        notBlank(roomName, "Please choose a room for the new section!");
        notBlank(facultyNumber, "Please choose an instructor for the new section!");
        if (sectionRepo.findById(sectionId).isPresent()) {
            throw new EnlistmentException("A section with section id " + sectionId + " already exists.");
        }

        var instructor = facultyRepository.findById(Integer.parseInt(facultyNumber)).orElseThrow(() ->
                new NoSuchElementException("No faculty found for facultyNumber " + facultyNumber));
        Period period = new Period(LocalTime.parse(start), LocalTime.parse(end));
        Schedule schedule = new Schedule(days, period);
        Subject subject = subjectRepo.findById(subjectId).orElseThrow(() -> new NoSuchElementException("No subject found with subjectId " + subjectId));
        Room room = roomRepo.findById(roomName).orElseThrow(() -> new NoSuchElementException("No room found with roomName " + roomName));
        Section section = new Section(sectionId, subject, schedule, room, instructor);
        List<Section> sections = sectionRepo.findAll();
        sections.forEach(currSection -> currSection.checkScheduleAndInstructor(section));
        sectionRepo.save(section);
        redirectAttrs.addFlashAttribute("sectionSuccessMessage", "New section successfully created!");
        return "redirect:sections";
    }

    @ExceptionHandler(EnlistmentException.class)
    public String handleException(RedirectAttributes redirectAttrs, EnlistmentException e) {
        redirectAttrs.addFlashAttribute("sectionExceptionMessage", e.getMessage());
        return "redirect:sections";
    }

    @ExceptionHandler(NullPointerException.class)
    public String handleException(RedirectAttributes redirectAttrs, NullPointerException e) {
        redirectAttrs.addFlashAttribute("sectionExceptionMessage", e.getMessage());
        return "redirect:sections";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleException(RedirectAttributes redirectAttrs, IllegalArgumentException e) {
        redirectAttrs.addFlashAttribute("sectionExceptionMessage", e.getMessage());
        return "redirect:sections";
    }

    void setSubjectRepo(SubjectRepository subjectRepo) {
        this.subjectRepo = subjectRepo;
    }

    void setSectionRepo(SectionRepository sectionRepo) {
        this.sectionRepo = sectionRepo;
    }

    void setRoomRepo(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    void setAdminRepo(AdminRepository adminRepo) {
        this.adminRepo = adminRepo;
    }

    void setFacultyRepository(FacultyRepository facultyRepo) {
        this.facultyRepository = facultyRepo;
    }

}
