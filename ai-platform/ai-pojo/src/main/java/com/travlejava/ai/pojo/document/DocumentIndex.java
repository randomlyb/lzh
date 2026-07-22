
package com.travlejava.ai.pojo.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIndex {
    private String id;
    private Long documentId;
    private Long knowledgeBaseId;
    private Integer chunkNo;
    private String content;
    private List<Float> vector;
    private Map<String, Object> metadata;
}
