package org.allsparks.mimic.control;

/**
 * Cross-mechanism bit a law may read. The diverter index treats
 * {@link #permitted()} as "the launcher is ready." Setting the bit does not
 * move a motor.
 */
public final class AttachmentContext {
    private boolean permitted;

    public AttachmentContext(boolean permitted) {
        this.permitted = permitted;
    }

    /** Fail-safe default: motion that depends on another mechanism stays blocked. */
    public static AttachmentContext blocked() {
        return new AttachmentContext(false);
    }

    /** Stick and flywheel laws that do not wait on another mechanism. */
    public static AttachmentContext open() {
        return new AttachmentContext(true);
    }

    public void permit(boolean permitted) {
        this.permitted = permitted;
    }

    public boolean permitted() {
        return permitted;
    }
}
