-- Create the ai_db database if it doesn't exist
CREATE DATABASE IF NOT EXISTS ai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_db;

-- Table for Knowledge Bases
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    name VARCHAR(255) NOT NULL COMMENT 'Knowledge Base Name',
    description TEXT COMMENT 'Knowledge Base Description',
    tags JSON COMMENT 'Tags for the knowledge base (JSON array of strings)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last Update Timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge Bases';

-- Table to link Knowledge Bases with Documents
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    knowledge_base_id BIGINT NOT NULL COMMENT 'Foreign Key to knowledge_base',
    document_id BIGINT NOT NULL COMMENT 'Foreign Key to document_metadata (from ai-service Document Center)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp',
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
    -- FOREIGN KEY (document_id) REFERENCES document_metadata(id) ON DELETE CASCADE -- This foreign key will be added once document_metadata table is defined if not already
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Links Knowledge Bases and Documents';

-- Table for Document Metadata (from ai-service Document Center)
-- This table might already be implicitly handled by the Document module's entities.
-- Re-defining here for completeness of ai_db schema if it's not explicitly created elsewhere.
CREATE TABLE IF NOT EXISTS document_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    file_name VARCHAR(255) NOT NULL COMMENT 'Original file name',
    file_path VARCHAR(1024) NOT NULL COMMENT 'Path to the stored file',
    file_size BIGINT COMMENT 'Size of the file in bytes',
    file_type VARCHAR(100) COMMENT 'MIME type or extension',
    status VARCHAR(50) DEFAULT 'UPLOADED' COMMENT 'Processing status (e.g., UPLOADED, PARSING, PARSED, FAILED)',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload timestamp',
    parsed_time DATETIME COMMENT 'Parsing completion timestamp',
    user_id BIGINT COMMENT 'ID of the user who uploaded the document',
    tags JSON COMMENT 'Tags associated with the document (JSON array of strings)',
    description TEXT COMMENT 'Document description',
    vector_index_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'Status of vector indexing (e.g., PENDING, INDEXING, INDEXED, FAILED)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last Update Timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document Metadata';

-- Table for Document Chunks (from ai-service Document Center)
-- This table might already be implicitly handled by the Document module's entities.
-- Re-defining here for completeness of ai_db schema if it's not explicitly created elsewhere.
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    document_id BIGINT NOT NULL COMMENT 'Foreign Key to document_metadata',
    chunk_text TEXT NOT NULL COMMENT 'Text content of the chunk',
    chunk_order INT NOT NULL COMMENT 'Order of the chunk within the document',
    vector_id VARCHAR(255) COMMENT 'ID of the vector in the vector database (e.g., Milvus, Elasticsearch)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last Update Timestamp',
    FOREIGN KEY (document_id) REFERENCES document_metadata(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document Chunks for RAG';

-- Table for Chat History (for RAG module)
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    user_id BIGINT NOT NULL COMMENT 'ID of the user',
    session_id VARCHAR(255) NOT NULL COMMENT 'Chat session ID',
    question TEXT NOT NULL COMMENT 'User question',
    answer TEXT NOT NULL COMMENT 'AI generated answer',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG Chat History';

-- Table for Vector Mapping (if needed to link internal IDs to external vector store IDs)
CREATE TABLE IF NOT EXISTS vector_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    entity_type VARCHAR(100) NOT NULL COMMENT 'Type of entity (e.g., DOCUMENT_CHUNK, KNOWLEDGE_BASE)',
    entity_id BIGINT NOT NULL COMMENT 'ID of the entity (e.g., document_chunk.id)',
    vector_store_name VARCHAR(100) NOT NULL COMMENT 'Name of the vector store (e.g., Elasticsearch, Milvus)',
    vector_id VARCHAR(255) NOT NULL COMMENT 'ID of the vector in the external vector store',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation Timestamp',
    UNIQUE KEY uk_entity_vector (entity_type, entity_id, vector_store_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Mapping of Internal Entities to External Vector IDs';
