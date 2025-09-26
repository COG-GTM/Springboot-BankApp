package com.example.bankapp.micronaut.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

import java.util.Map;

@Controller("/api/test")
public class TestController {

    @Get
    public HttpResponse<?> testGet() {
        return HttpResponse.ok().body(Map.of("message", "Micronaut GET test endpoint is working"));
    }

    @Post
    public HttpResponse<?> testPost() {
        return HttpResponse.ok().body(Map.of("message", "Micronaut POST test endpoint is working"));
    }
}
