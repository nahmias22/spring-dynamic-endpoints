package com.example.demo.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.TestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RequestMapping(path = "/api/v1.0/test")
@Tag(name = "Non Reactive Controller")
public class TestController {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get result:", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TestDTO.class))),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public TestDTO test() {
        throw new RuntimeException("test");
//        return new TestDTO("ok", 200L);
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Post Request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Made request:", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<String> testPost() {
        return Mono.just("ok");
    }

    @RequestMapping(path="/reactive",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> testw() {
        return Mono.just("ok1");
    }

    @RequestMapping(method = RequestMethod.GET, path = "/suppliers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get result", description = FIND_ALL_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get returned:"),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<TestDTO> getSuppliers(@ParameterObject
                                                   @Schema(description = "Fill out fields to filter by")
                                          TestDTO search,
                                                   @RequestParam(value = "strict", required = false) @Schema(description = "Search type", implementation = Boolean.class) Boolean strict) {
        return Mono.just(new TestDTO("test", 100L));
    }

    private static final String FIND_ALL_DESCRIPTION = """
            You can fill out the different fields to search by or set the param strict to true and it will retrieve all 
            entries in a paged format.""";
}
