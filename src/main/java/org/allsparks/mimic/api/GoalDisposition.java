package org.allsparks.mimic.api;

/** How a requested goal was handled. Every non-identity outcome needs a reason. */
public enum GoalDisposition {
    ACCEPTED,
    REJECTED,
    DEFERRED,
    CLAMPED,
    REPLACED
}
