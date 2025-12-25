package com.jpmc.midascore.service;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final DatabaseConduit databaseConduit;
    private final IncentiveService incentiveService;

    public TransactionService(DatabaseConduit databaseConduit, IncentiveService incentiveService) {
        this.databaseConduit = databaseConduit;
        this.incentiveService = incentiveService;
    }

    /**
     * Processes a transaction with incentive integration.
     * 
     * @param transaction The transaction to process
     * @return true if transaction was processed successfully, false if validation failed
     */
    public boolean processTransaction(Transaction transaction) {
        // Step 1: Fetch entities
        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());
        float amount = transaction.getAmount();

        // Step 2: Validation
        if (sender == null || recipient == null || sender.getBalance() < amount) {
            logger.info(
                    "Transaction discarded. sender={}, recipient={}, amount={}",
                    sender != null ? sender.getName() : "null",
                    recipient != null ? recipient.getName() : "null",
                    amount
            );
            return false;
        }

        // Step 3: Fetch incentive from Incentive API (after validation)
        float incentive = 0.0f;
        try {
            incentive = incentiveService.getIncentive(transaction);
            transaction.setIncentive(incentive);
        } catch (Exception e) {
            logger.error("Failed to fetch incentive, continuing without incentive: {}", e.getMessage());
            // Continue processing without incentive if API fails
            transaction.setIncentive(0.0f);
        }

        // Step 4: Apply balance changes
        // Sender's balance decreases by transaction amount only
        sender.setBalance(sender.getBalance() - amount);
        
        // Recipient's balance increases by transaction amount + incentive
        recipient.setBalance(recipient.getBalance() + amount + incentive);

        // Step 5: Persist transaction record
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount);
        
        databaseConduit.save(sender);
        databaseConduit.save(recipient);
        databaseConduit.save(transactionRecord);

        // Step 6: Log success
        logger.info(
                "Transaction applied: {} -> {} | amount={} | incentive={} | senderBalance={} | recipientBalance={}",
                sender.getName(),
                recipient.getName(),
                amount,
                incentive,
                sender.getBalance(),
                recipient.getBalance()
        );

        return true;
    }
}
