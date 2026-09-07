package com.travlejava.taskscheduler.service.election;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Service
public class LeaderElectionService extends LeaderSelectorListenerAdapter implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);
    private static final String LEADER_PATH = "/scheduler/leader";

    private final LeaderSelector leaderSelector;
    private final CuratorFramework client;
    private final String instanceId;
    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile boolean isLeader = false;

    public LeaderElectionService(CuratorFramework client) {
        this.client = client;
        this.instanceId = "scheduler-instance-" + System.nanoTime(); // Unique ID for this instance
        this.leaderSelector = new LeaderSelector(client, LEADER_PATH, this);
        this.leaderSelector.autoRequeue(); // Ensure that the instance will be recandidate for leadership if it loses it
    }

    @PostConstruct
    public void start() throws Exception {
        logger.info("Starting leader election for instance: {}", instanceId);
        leaderSelector.start();
    }

    @Override
    public void takeLeadership(CuratorFramework client) throws Exception {
        logger.info("{} is now the leader!", instanceId);
        isLeader = true;
        try {
            latch.await(); // Keep the leader alive until interrupted or closed
        } catch (InterruptedException e) {
            logger.warn("{} leadership interrupted.", instanceId, e);
            Thread.currentThread().interrupt();
        } finally {
            logger.info("{} relinquishing leadership.", instanceId);
            isLeader = false;
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (newState == ConnectionState.LOST || newState == ConnectionState.SUSPENDED) {
            isLeader = false;
            logger.warn("{} lost/suspended connection, relinquishing leadership.", instanceId);
            latch.countDown(); // Release leadership
        } else if (newState == ConnectionState.RECONNECTED) {
            logger.info("{} reconnected, re-entering leader election.", instanceId);
            // autoRequeue handles re-candidacy, so no explicit action needed here
        }
    }

    public boolean isLeader() {
        return isLeader;
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        logger.info("Closing leader selector for instance: {}", instanceId);
        leaderSelector.close();
        latch.countDown(); // Ensure that if we are the leader, we relinquish it
    }
}