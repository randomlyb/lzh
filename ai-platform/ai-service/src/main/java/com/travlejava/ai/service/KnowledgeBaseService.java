package com.travlejava.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travlejava.ai.pojo.document.DocumentMetadata;
import com.travlejava.ai.pojo.knowledge.KnowledgeBase;
import com.travlejava.ai.pojo.knowledge.KnowledgeDocument;
import com.travlejava.ai.pojo.knowledge.dto.AddDocumentToKbRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseResponse;
import com.travlejava.ai.service.mapper.DocumentMapper;
import com.travlejava.ai.service.mapper.KnowledgeBaseMapper;
import com.travlejava.ai.service.mapper.KnowledgeDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentMapper documentMapper;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper, KnowledgeDocumentMapper knowledgeDocumentMapper, DocumentMapper documentMapper) {
        super(knowledgeBaseMapper);
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(request, knowledgeBase);
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        baseMapper.insert(knowledgeBase);
        log.info("Created knowledge base: {}", knowledgeBase.getName());
        return convertToResponse(knowledgeBase);
    }

    public List<KnowledgeBaseResponse> getAllKnowledgeBases() {
        return list().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public KnowledgeBaseResponse getKnowledgeBaseById(Long id) {
        KnowledgeBase knowledgeBase = getById(id);
        return knowledgeBase != null ? convertToResponse(knowledgeBase) : null;
    }

    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            return null;
        }
        BeanUtils.copyProperties(request, knowledgeBase);
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(knowledgeBase);
        log.info("Updated knowledge base: {}", knowledgeBase.getName());
        return convertToResponse(knowledgeBase);
    }

    @Transactional
    public boolean deleteKnowledgeBase(Long id) {
        // Also delete associated knowledge documents
        knowledgeDocumentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getKnowledgeBaseId, id));
        boolean deleted = removeById(id);
        if (deleted) {
            log.info("Deleted knowledge base with ID: {}", id);
        }
        return deleted;
    }

    @Transactional
    public boolean addDocumentsToKnowledgeBase(AddDocumentToKbRequest request) {
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        List<Long> documentIds = request.getDocumentIds();

        if (knowledgeBaseId == null || documentIds == null || documentIds.isEmpty()) {
            return false;
        }

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            log.warn("Knowledge base with ID {} not found.", knowledgeBaseId);
            return false;
        }

        for (Long documentId : documentIds) {
            DocumentMetadata document = documentMapper.selectById(documentId);
            if (document == null) {
                log.warn("Document with ID {} not found. Skipping.", documentId);
                continue;
            }
            // Check if already added
            if (knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                    .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                    .eq(KnowledgeDocument::getDocumentId, documentId)) > 0) {
                log.info("Document {} already in knowledge base {}. Skipping.", documentId, knowledgeBaseId);
                continue;
            }

            KnowledgeDocument knowledgeDocument = new KnowledgeDocument();
            knowledgeDocument.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeDocument.setDocumentId(documentId);
            knowledgeDocument.setCreatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.insert(knowledgeDocument);
            log.info("Added document {} to knowledge base {}.", documentId, knowledgeBaseId);
        }
        return true;
    }

    @Transactional
    public boolean removeDocumentFromKnowledgeBase(Long knowledgeBaseId, Long documentId) {
        int deletedRows = knowledgeDocumentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocument::getDocumentId, documentId));
        if (deletedRows > 0) {
            log.info("Removed document {} from knowledge base {}.", documentId, knowledgeBaseId);
            return true;
        }
        return false;
    }

    private KnowledgeBaseResponse convertToResponse(KnowledgeBase knowledgeBase) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        BeanUtils.copyProperties(knowledgeBase, response);
        Integer documentCount = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBase.getId()));
        response.setDocumentCount(documentCount);
        return response;
    }
}
