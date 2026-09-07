package com.travlejava.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travlejava.ai.pojo.document.entity.DocumentMetadata;
import com.travlejava.ai.pojo.knowledge.KnowledgeBase;
import com.travlejava.ai.pojo.knowledge.KnowledgeDocument;
import com.travlejava.ai.pojo.knowledge.dto.AddDocumentToKbRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseRequest;
import com.travlejava.ai.pojo.knowledge.dto.KnowledgeBaseResponse;
import com.travlejava.ai.service.mapper.DocumentMapper;
import com.travlejava.ai.service.mapper.KnowledgeBaseMapper;
import com.travlejava.ai.service.mapper.KnowledgeDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentMapper documentMapper;

    public KnowledgeBaseService(KnowledgeDocumentMapper knowledgeDocumentMapper, DocumentMapper documentMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentMapper = documentMapper;
    }

    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(request, knowledgeBase);
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(knowledgeBase);
        logger.info("Created knowledge base: {}", knowledgeBase.getName());
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
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(knowledgeBase);
        logger.info("Updated knowledge base: {}", knowledgeBase.getName());
        return convertToResponse(knowledgeBase);
    }

    @Transactional
    public boolean deleteKnowledgeBase(Long id) {
        knowledgeDocumentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getKnowledgeBaseId, id));
        boolean deleted = removeById(id);
        if (deleted) {
            logger.info("Deleted knowledge base with ID: {}", id);
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
            logger.warn("Knowledge base with ID {} not found.", knowledgeBaseId);
            return false;
        }

        for (Long documentId : documentIds) {
            DocumentMetadata document = documentMapper.selectById(documentId);
            if (document == null) {
                logger.warn("Document with ID {} not found. Skipping.", documentId);
                continue;
            }
            Long existing = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                    .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                    .eq(KnowledgeDocument::getDocumentId, documentId));
            if (existing != null && existing > 0) {
                logger.info("Document {} already in knowledge base {}. Skipping.", documentId, knowledgeBaseId);
                continue;
            }

            KnowledgeDocument knowledgeDocument = new KnowledgeDocument();
            knowledgeDocument.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeDocument.setDocumentId(documentId);
            knowledgeDocument.setCreateTime(LocalDateTime.now());
            knowledgeDocumentMapper.insert(knowledgeDocument);
            logger.info("Added document {} to knowledge base {}.", documentId, knowledgeBaseId);
        }
        return true;
    }

    @Transactional
    public boolean removeDocumentFromKnowledgeBase(Long knowledgeBaseId, Long documentId) {
        int deletedRows = knowledgeDocumentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocument::getDocumentId, documentId));
        if (deletedRows > 0) {
            logger.info("Removed document {} from knowledge base {}.", documentId, knowledgeBaseId);
            return true;
        }
        return false;
    }

    private KnowledgeBaseResponse convertToResponse(KnowledgeBase knowledgeBase) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        BeanUtils.copyProperties(knowledgeBase, response);
        response.setCreatedAt(knowledgeBase.getCreateTime());
        response.setUpdatedAt(knowledgeBase.getUpdateTime());
        Long documentCount = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBase.getId()));
        response.setDocumentCount(documentCount != null ? documentCount.intValue() : 0);
        return response;
    }
}