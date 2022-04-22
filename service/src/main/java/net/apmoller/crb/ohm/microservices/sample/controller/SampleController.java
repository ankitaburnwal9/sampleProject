package net.apmoller.crb.ohm.microservices.sample.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.apmoller.crb.ohm.microservices.sample.models.User;
import net.apmoller.crb.ohm.microservices.sample.services.SubmissionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
class SampleController {

    private final SubmissionService submissionService;

    @PostMapping(path = "/user")
    public User getUser(@RequestParam(name = "name") String user_name, @RequestParam(value = "dept") String user_dept) {

        log.info("request received for SampleController");

        return submissionService.submit(user_name, user_dept);
    }
}
