package com.expensetracker.web;

import com.expensetracker.dto.RecurringDtos.RecurringRequest;
import com.expensetracker.dto.RecurringDtos.RecurringResponse;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.RecurringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring")
public class RecurringController {

    private final RecurringService recurring;

    public RecurringController(RecurringService recurring) {
        this.recurring = recurring;
    }

    @GetMapping
    public List<RecurringResponse> list() {
        return recurring.list(CurrentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse create(@Valid @RequestBody RecurringRequest req) {
        return recurring.create(CurrentUser.id(), req);
    }

    @PutMapping("/{id}")
    public RecurringResponse update(@PathVariable Long id, @Valid @RequestBody RecurringRequest req) {
        return recurring.update(CurrentUser.id(), id, req);
    }

    @PostMapping("/{id}/pause")
    public RecurringResponse pause(@PathVariable Long id) {
        return recurring.pause(CurrentUser.id(), id);
    }

    @PostMapping("/{id}/resume")
    public RecurringResponse resume(@PathVariable Long id) {
        return recurring.resume(CurrentUser.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        recurring.delete(CurrentUser.id(), id);
    }
}
