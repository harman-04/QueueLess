//package com.queueless.backend.controller;
//
//import com.queueless.backend.model.Queue;
//import com.queueless.backend.service.QueueService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/provider/queues")
//@RequiredArgsConstructor
//public class ProviderController {
//
//    private final QueueService queueService;
//
//    @PostMapping
//    public ResponseEntity<Queue> createQueue(@RequestBody Queue queue) {
//        return ResponseEntity.ok(queueService.createQueue(queue));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Queue>> getQueues(@RequestParam String providerId) {
//        return ResponseEntity.ok(queueService.getQueuesByProvider(providerId));
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Optional<Queue>> updateQueue(@PathVariable String id, @RequestBody Queue queue) {
//        return ResponseEntity.ok(queueService.updateQueue(id, queue));
//    }
//
//    @PatchMapping("/{id}/close")
//    public ResponseEntity<Void> closeQueue(@PathVariable String id) {
//        queueService.closeQueue(id);
//        return ResponseEntity.noContent().build();
//    }
//}
