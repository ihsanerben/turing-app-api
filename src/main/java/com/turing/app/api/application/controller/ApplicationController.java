package com.turing.app.api.application.controller;

import com.turing.app.api.application.dto.*;
import com.turing.app.api.application.service.ApplicationService;
import com.turing.app.api.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/me/applications")
public class ApplicationController {
    private final ApplicationService service; public ApplicationController(ApplicationService service){this.service=service;}
    @GetMapping public List<ApplicationResponse> list(@AuthenticationPrincipal AuthenticatedUser user){return service.list(user.id());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApplicationResponse create(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody ApplicationCreateRequest body){return service.create(user.id(),body);}
    @GetMapping("/{id}") public ApplicationResponse get(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){return service.get(user.id(),id);}
    @GetMapping("/{id}/form") public ApplicationFormResponse form(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){return service.form(user.id(),id);}
    @PutMapping("/{id}/answers") public ApplicationFormResponse answers(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody AnswersUpdateRequest body){return service.saveAnswers(user.id(),id,body);}
    @PostMapping("/{id}/submit") public ApplicationResponse submit(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody ApplicationVersionRequest body){return service.submit(user.id(),id,body);}
    @PostMapping("/{id}/withdraw") public ApplicationResponse withdraw(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody ApplicationVersionRequest body){return service.withdraw(user.id(),id,body);}
}
