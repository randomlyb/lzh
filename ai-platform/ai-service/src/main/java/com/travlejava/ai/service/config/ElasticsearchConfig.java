package com.travlejava.ai.service.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.travlejava.ai.service.repository") // Assuming repositories will be here
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    // This URL will be read from application.yml: spring.elasticsearch.uris
    // We don't need to explicitly define it here if Spring Boot autoconfigures it.
    // However, for explicit control or custom client, we can define a bean.

    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedToLocalhost() // Assuming default localhost:9200 from application.yml
                .build();
        return RestClients.create(clientConfiguration).rest();
    }
}
