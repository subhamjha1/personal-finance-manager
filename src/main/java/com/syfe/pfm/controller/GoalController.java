package com.syfe.pfm.controller;

import com.syfe.pfm.dto.GoalDtos.GoalListResponse;
import com.syfe.pfm.dto.GoalDtos.GoalRequest;
import com.syfe.pfm.dto.GoalDtos.GoalResponse;
import com.syfe.pfm.dto.GoalDtos.GoalUpdateRequest;
import com.syfe.pfm.dto.MessageResponse;
import com.syfe.pfm.security.UserPrincipal;
import com.syfe.pfm.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Savings-goal endpoints. */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody GoalRequest request) {
        return goalService.create(principal.getId(), request);
    }

    @GetMapping
    public GoalListResponse list(@AuthenticationPrincipal UserPrincipal principal) {
        return goalService.list(principal.getId());
    }

    @GetMapping("/{id}")
    public GoalResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return goalService.get(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                               @Valid @RequestBody GoalUpdateRequest request) {
        return goalService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        goalService.delete(principal.getId(), id);
        return new MessageResponse("Goal deleted successfully");
    }
}
