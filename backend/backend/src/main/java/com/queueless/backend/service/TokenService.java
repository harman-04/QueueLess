//package com.queueless.backend.service;
//
//import com.queueless.backend.model.Token;
//import com.queueless.backend.repository.TokenRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class TokenService {
//
//    private final TokenRepository tokenRepository;
//    private final MLService mlService;
//
//    public List<Token> getTokensByQueue(String queueId) {
//        return tokenRepository.findByQueueIdOrderByTokenNumberAsc(queueId);
//    }
//
//    public Optional<Token> serveToken(String id) {
//        return tokenRepository.findById(id).map(token -> {
//            token.setStatus("SERVED");
//            return tokenRepository.save(token);
//        });
//    }
//
//    public Optional<Token> skipToken(String id) {
//        return tokenRepository.findById(id).map(token -> {
//            token.setStatus("SKIPPED");
//            return tokenRepository.save(token);
//        });
//    }
//
//    public Token issueToken(Token token) {
//        List<Token> tokens = tokenRepository.findByQueueIdOrderByTokenNumberAsc(token.getQueueId());
//        int nextTokenNumber = tokens.size() + 1;
//        token.setTokenNumber(nextTokenNumber);
//        token.setIssuedAt(LocalDateTime.now());
//
//        int hourOfDay = LocalDateTime.now().getHour();
//        // Call ML model for estimating wait time
//        int estimatedWait = (int) mlService.predictWaitTime(token.getQueueId(), nextTokenNumber,hourOfDay);
//        token.setEstimatedServingTime(LocalDateTime.now().plusMinutes(estimatedWait));
//        token.setStatus("PENDING");
//        return tokenRepository.save(token);
//    }
//}
