package com.travlejava.taskscheduler.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long documentId;
    private String documentPath;
    private String fileType; // e.g., PDF, DOCX, TXT
    // Other relevant metadata for parsing
}
