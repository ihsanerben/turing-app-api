package com.turing.app.api.scholarship.controller;
import com.turing.app.api.scholarship.dto.PublicScholarshipResponse;
import com.turing.app.api.scholarship.service.ScholarshipService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/public/scholarships")
public class PublicScholarshipController{private final ScholarshipService service;public PublicScholarshipController(ScholarshipService service){this.service=service;}@GetMapping public List<PublicScholarshipResponse> list(){return service.publicPrograms();}@GetMapping("/{slug}") public PublicScholarshipResponse detail(@PathVariable String slug){return service.publicProgram(slug);}}
