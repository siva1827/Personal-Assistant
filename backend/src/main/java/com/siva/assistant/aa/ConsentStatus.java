package com.siva.assistant.aa;

public enum ConsentStatus {
    /** Created; waiting for the user to approve it in their Account Aggregator app. */
    PENDING,
    ACTIVE,
    REJECTED,
    /** The user withdrew permission. No more data can be fetched. */
    REVOKED,
    /** The permission ran out of time. */
    EXPIRED
}
