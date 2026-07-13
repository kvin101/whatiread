package com.whatiread.web;

import com.whatiread.shared.web.ApiPaths;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
public class StatusController {

    @GetMapping("/status")
    ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "whatiread",
                "status", "ok"
        ));
    }
}
