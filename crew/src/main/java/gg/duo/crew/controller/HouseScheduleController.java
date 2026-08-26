package gg.duo.crew.controller;

import gg.duo.crew.dto.ScheduleDto;
import gg.duo.crew.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crew/houses/{houseId}/schedules")
@RequiredArgsConstructor
public class HouseScheduleController {

    private final ScheduleService scheduleService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    @GetMapping
    public List<ScheduleDto.Response> list(@PathVariable Long houseId, Authentication auth) {
        return scheduleService.list(houseId, userId(auth));
    }

    @PostMapping
    public ScheduleDto.Response create(@PathVariable Long houseId, Authentication auth,
                                       @Valid @RequestBody ScheduleDto.WriteRequest req) {
        return scheduleService.create(houseId, userId(auth), req);
    }

    @PostMapping("/{scheduleId}/participants")
    public ScheduleDto.Response join(@PathVariable Long houseId, @PathVariable Long scheduleId,
                                     Authentication auth) {
        return scheduleService.join(houseId, scheduleId, userId(auth));
    }

    @DeleteMapping("/{scheduleId}/participants")
    public ScheduleDto.Response leave(@PathVariable Long houseId, @PathVariable Long scheduleId,
                                      Authentication auth) {
        return scheduleService.leave(houseId, scheduleId, userId(auth));
    }
}
