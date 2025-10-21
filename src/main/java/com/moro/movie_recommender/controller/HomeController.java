package com.moro.movie_recommender.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Handles the home page redirect.
 */
@Controller
public class HomeController {

    /**
     * Redirects the root path to the index.html static file.
     * 
     * @return redirect to /index.html
     */
    @GetMapping("/")
    public Mono<ResponseEntity<Void>> home() {
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/index.html"))
                .build());
    }
}
