package org.example;

import java.util.concurrent.ConcurrentHashMap;

public class Ledger {

    private final ConcurrentHashMap<String, Double> accounts = new ConcurrentHashMap<>();

    int counter = 1;

    public Ledger() {}

    public synchronized boolean transfer(Transaction transaction) {

        accounts.putIfAbsent(transaction.from(), 1000.0);
        accounts.putIfAbsent(transaction.to(), 1000.0);

        double fromBalance = accounts.get(transaction.from());
        double toBalance = accounts.get(transaction.to());

        int transactionId = counter++;

        System.out.println("\n--- Transaction Request " + transactionId + " ---");
        System.out.println("Thread: " + Thread.currentThread());
        System.out.println("From: " + transaction.from());
        System.out.println("To: " + transaction.to());
        System.out.println("Amount: " + transaction.amount());
        System.out.println("Balances BEFORE -> " + transaction.from() + ": " + fromBalance +
                ", " + transaction.to() + ": " + toBalance);

        if (fromBalance < transaction.amount()) {
            System.out.println("Transaction error: Insufficient Funds!");
            return false;
        }

        double newFromBalance = fromBalance - transaction.amount();
        double newToBalance = toBalance + transaction.amount();

        accounts.put(transaction.from(), newFromBalance);
        accounts.put(transaction.to(), newToBalance);

        System.out.println("Balances AFTER -> " +
                transaction.from() + ": " + newFromBalance +
                ", " + transaction.to() + ": " + newToBalance);
        System.out.println("----------------------------\n");

        return true;
    }
}