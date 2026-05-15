package com.example.tradingdesk.service;

import com.example.tradingdesk.domain.Trader;
import com.example.tradingdesk.repository.TraderRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TraderService {

    private final TraderRepository traderRepository;
    private final ConcurrentHashMap<String, ReentrantLock> creationLocks = new ConcurrentHashMap<>();

    public TraderService(TraderRepository traderRepository) {
        this.traderRepository = traderRepository;
    }

    public Trader getOrCreateLocked(String traderId) {
        String normalizedTraderId = SymbolNormalizer.normalize(traderId);
        return traderRepository.findByTraderIdForUpdate(normalizedTraderId)
                .orElseGet(() -> createThenLock(normalizedTraderId));
    }

    private Trader createThenLock(String traderId) {
        ReentrantLock creationLock = creationLocks.computeIfAbsent(traderId, ignored -> new ReentrantLock());
        creationLock.lock();
        boolean releaseAfterTransaction = TransactionSynchronizationManager.isSynchronizationActive();
        if (releaseAfterTransaction) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    unlockCreationLock(traderId, creationLock);
                }
            });
        }
        try {
            traderRepository.findByTraderId(traderId)
                    .orElseGet(() -> traderRepository.saveAndFlush(new Trader(traderId)));
        } finally {
            if (!releaseAfterTransaction) {
                unlockCreationLock(traderId, creationLock);
            }
        }
        return traderRepository.findByTraderIdForUpdate(traderId)
                .orElseThrow(() -> new IllegalStateException("Could not create trader " + traderId));
    }

    private void unlockCreationLock(String traderId, ReentrantLock creationLock) {
        creationLock.unlock();
        if (!creationLock.hasQueuedThreads()) {
            creationLocks.remove(traderId, creationLock);
        }
    }
}
