package gg.duo.crew.controller;

import gg.duo.crew.dto.HouseRankingDto;
import gg.duo.crew.service.HouseRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crew/houses/rankings")
@RequiredArgsConstructor
public class HouseRankingController {

    private final HouseRankingService rankingService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<HouseRankingDto.Response> rankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rankingService.getRankings(page, size));
    }

    @GetMapping("/me")
    public ResponseEntity<List<HouseRankingDto.Item>> myRankings(Authentication auth) {
        return ResponseEntity.ok(rankingService.getMyRankings(userId(auth)));
    }
}
