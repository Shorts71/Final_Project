# Financial Messaging Server

## Overview

This project features a high-concurrency financial messaging server in Java.  
The server processes financial transactions between accounts over HTTP via a ledger.

Clients send transaction requests in JSON format and the server processes them concurrently using Java Virtual Threads.

The goal of the project is to demonstrate:

- High-concurrency request handling
- Network communication using HTTP
- Thread-safe transaction processing
- Handling of simultaneous thread requests

---

# System Architecture

The system consists of three primary components:

```
Client (PowerShell / curl)
        │
        │ HTTP POST /transaction
        ▼
HTTP Server (Java HttpServer)
        │
        │ parses JSON request
        ▼
Transaction Object
        │
        ▼
     Ledger
        │
        ▼
Account Balances Updated
```

## Components

### 1. HTTP Server (`Main.java`)

The server is built using Java's built-in `HttpServer`.

Responsibilities:

- Listen for incoming HTTP requests
- Parse JSON transaction data
- Forward transactions to the ledger
- Return responses to the client

The server exposes a single endpoint:

```
POST /transaction
```

Example request body:

```json
{
  "from": "RBC",
  "to": "TD",
  "amount": 100
}
```

---

### 2. Transaction Record (`Transaction.java`)

Transactions are represented using a Java record.

Each transaction contains these values:

- From: the source account.
- To: the Destination account.
- Amount: the amount to be transferred from the source to the destination.

---

### 3. Ledger (`Ledger.java`)

The ledger maintains account balances using a Concurrent Hashmap.

Responsibilities:

- Store account balances
- Validate transaction funds
- Update balances
- Ensure thread safety

The ledger's transfer method is synchronized to prevent race conditions during the process.

---

# Concurrency Design

Each HTTP request is processed on its own thread.

This allows the server to process many requests simultaneously while maintaining safe access to shared data.

---

# Network Protocol Implementation

The server communicates using **HTTP over TCP**.

### Request

Method:

```
POST /transaction
```

Headers:

```
Content-Type: application/json
```

Body:

```json
{
  "from": "RBC",
  "to": "CIBC",
  "amount": 50
}
```

### Response

Success:

```
Transaction successful
```

Failure:

```
Insufficient funds
```

### Error Handling

If a request is invalid or the server encounters an error:

```
HTTP 500
Server error: <message>
```

---

# JSON Parsing

JSON parsing is done via the extract method in Main which locates key/value pairs in the request body and extracts their values.

The extracted values are used to construct a Transaction object.

This was done to prevent the addition of too many packages/dependencies.

---

# Load Testing

The following script is used in Powershell to simulate thousands of simultaneous requests and test concurrency:

```powershell
1..1000 | ForEach-Object {

    Start-Job -ScriptBlock {

        $accounts = @("RBC","CIBC","Scotiabank","BMO","TD")

        $from = Get-Random -InputObject $accounts
        $to = Get-Random -InputObject $accounts

        while ($to -eq $from) {
            $to = Get-Random -InputObject $accounts
        }

        $amount = Get-Random -Minimum 1 -Maximum 500

        $body = @{
            from = $from
            to = $to
            amount = $amount
        } | ConvertTo-Json

        Invoke-RestMethod `
            -Uri "http://localhost:8080/transaction" `
            -Method Post `
            -Body $body `
            -ContentType "application/json"
    }
}
```

---

# Challenges Faced

## 1. Handling Concurrent Transactions

During testing, race conditions were encountered due to multiple threads attempting to manipulate a given account's balance at a time.

The synchronized keyword was added to the transfer method to ensure one thread at a time could modify the balances, preventing race conditions while still maintaining concurrency.

---

## 2. Parsing JSON Without External Libraries

Since no JSON library was used, parsing had to be implemented manually.

Challenges included:

- locating key/value pairs
- handling string vs numeric values
- ensuring valid parsing boundaries

After enough trial and error, a solution was found.

---

# Example Successful Server Output

```
--- Transaction Request 1 ---
Thread: VirtualThread[#32]
From: RBC
To: TD
Amount: 200
Balances BEFORE -> RBC: 1000.0, TD: 1000.0
Balances AFTER -> RBC: 800.0, TD: 1200.0
```

---

# How to Run

## Start the server

```
run Main.java
```

The server starts on:

```
http://localhost:8080
```

## Send a transaction

Use the Powershell script from above.
