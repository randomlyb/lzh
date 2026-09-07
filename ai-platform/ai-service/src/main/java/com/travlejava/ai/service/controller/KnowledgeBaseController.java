package com.travlejava.ai.service.controller;

import com.travlejava.ai.pojo.knowledge.dto.AddDocumentToKbRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseResponse;
import com.travlejava.ai.service.KnowledgeBaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ResponseEntity<KnowledgeBaseResponse> createKnowledgeBase(@RequestBody KnowledgeBaseRequest request) {
        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeBaseResponse>> getAllKnowledgeBases() {
        List<KnowledgeBaseResponse> knowledgeBases = knowledgeBaseService.getAllKnowledgeBases();
        return ResponseEntity.ok(knowledgeBases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseResponse> getKnowledgeBaseById(@PathVariable Long id) {
        KnowledgeBaseResponse knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id);
        return knowledgeBase != null ? ResponseEntity.ok(knowledgeBase) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseResponse> updateKnowledgeBase(@PathVariable Long id, @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBaseResponse response = knowledgeBaseService.updateKnowledgeBase(id, request);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(@PathVariable Long id) {
        boolean deleted = knowledgeBaseService.deleteKnowledgeBase(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/add-documents")
    public ResponseEntity<Void> addDocumentsToKnowledgeBase(@RequestBody AddDocumentToKbRequest request) {
        boolean success = knowledgeBaseService.addDocumentsToKnowledgeBase(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{knowledgeBaseId}/documents/{documentId}")
    public ResponseEntity<Void> removeDocumentFromKnowledgeBase(@PathVariable Long knowledgeBaseId, @PathVariable Long documentId) {
        knowledgeBaseService.removeDocumentFromKnowledgeBase(knowledgeBaseId, documentId);
        return ResponseEntity.noContent().build();
    }
}