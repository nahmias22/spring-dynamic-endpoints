package com.example.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.model.TestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RequestMapping(path = "/api/v2.0/test")
@Tag(name = "Test 2 Operations")
public class TestRController {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get test data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved data", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TestDTO.class))),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<TestDTO> getTest() {
        return Mono.just(new TestDTO("Retrieved data", 200L));
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create test data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Made request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<String> postTest() {
        return Mono.just("ok");
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update test data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated data", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TestDTO.class))),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<TestDTO> putTest(@RequestBody TestDTO testDTO) {
        return Mono.just(new TestDTO("Updated data", 200L));
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete test data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted data", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access Forbidden",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<Void> deleteTest() {
         return Mono.empty();
    }
}
