package com.jpmc.midascore.consumer;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    private final DatabaseConduit databaseConduit;

    public TransactionListener(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);

        // Step 2: Validation Phase
        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());
        float amount = transaction.getAmount();

        logger.info("Received transaction: {}", transaction);


        // Check all validation conditions
        if (sender == null || recipient == null || sender.getBalance() < amount) {
            return;
        }

        // Step 3: Persistence Phase - Create TransactionRecord
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount);

        // Step 4: Balance Update Phase - Update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        logger.info("Transaction applied: {} -> {} amount={}. New balances: sender={}, recipient={}",
            sender.getName(), recipient.getName(), amount, sender.getBalance(), recipient.getBalance());

        // Persist all changes atomically
        databaseConduit.save(sender);
        databaseConduit.save(recipient);
        databaseConduit.save(transactionRecord);
    }
}
