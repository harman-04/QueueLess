//package com.queueless.backend.controller;
//
//import com.queueless.backend.model.Token;
//import com.queueless.backend.service.TokenService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/provider")
//@RequiredArgsConstructor
//public class TokenController {
//
//    private final TokenService tokenService;
//
//    @GetMapping("/tokens")
//    public ResponseEntity<List<Token>> getTokensByQueue(@RequestParam("queueId") String queueId) {
//        List<Token> tokens = tokenService.getTokensByQueue(queueId);
//        return new ResponseEntity<>(tokens, HttpStatus.OK);
//    }
//
//    @PatchMapping("/tokens/{id}/serve")
//    public ResponseEntity<Token> serveToken(@PathVariable String id) {
//        return tokenService.serveToken(id)
//                .map(token -> new ResponseEntity<>(token, HttpStatus.OK))
//                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    @PatchMapping("/tokens/{id}/skip")
//    public ResponseEntity<Token> skipToken(@PathVariable String id) {
//        return tokenService.skipToken(id)
//                .map(token -> new ResponseEntity<>(token, HttpStatus.OK))
//                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    @PostMapping("/tokens")
//    public ResponseEntity<Token> issueToken(@RequestBody Token token) {
//        Token issuedToken = tokenService.issueToken(token);
//        return new ResponseEntity<>(issuedToken, HttpStatus.CREATED);
//    }
//}
