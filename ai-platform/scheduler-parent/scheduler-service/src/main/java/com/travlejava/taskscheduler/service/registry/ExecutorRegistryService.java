package com.travlejava.taskscheduler.service.registry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.travlejava.taskscheduler.common.entity.ExecutorInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExecutorRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorRegistryService.class);
    private static final String BASE_PATH = "/executors";
    private static final String SERVICE_NAME = "task-executor";

    private final CuratorFramework client;
    private ServiceDiscovery<ExecutorInfo> serviceDiscovery;

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.application.name}")
    private String applicationName;

    public ExecutorRegistryService(CuratorFramework client) {
        this.client = client;
    }

    @PostConstruct
    public void start() throws Exception {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ExecutorInfo.class)
                .client(client)
                .basePath(BASE_PATH)
                .build();
        serviceDiscovery.start();

        // Register this instance as an Executor (even if it's primarily a scheduler, it can also act as an executor)
        registerExecutor();
    }

    private void registerExecutor() throws Exception {
        String host = "localhost"; // For simplicity, assume localhost. In a real env, get actual IP.
        String id = applicationName + ":" + host + ":" + serverPort;

        ExecutorInfo executorInfo = new ExecutorInfo(host, serverPort, id);
        ServiceInstance<ExecutorInfo> serviceInstance = ServiceInstance.<ExecutorInfo>builder()
                .name(SERVICE_NAME)
                .id(id)
                .address(host)
                .port(serverPort)
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .payload(executorInfo)
                .build();

        serviceDiscovery.registerService(serviceInstance);
        logger.info("Registered executor: {}", id);
    }

    public List<ExecutorInfo> getAvailableExecutors() {
        try {
            Collection<ServiceInstance<ExecutorInfo>> instances = serviceDiscovery.queryForInstances(SERVICE_NAME);
            return instances.stream()
                    .map(ServiceInstance::getPayload)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error querying for executor instances", e);
            return List.of();
        }
    }

    @PreDestroy
    public void close() throws IOException {
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }
    }
}
