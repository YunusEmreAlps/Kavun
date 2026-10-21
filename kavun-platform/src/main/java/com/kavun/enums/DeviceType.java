package com.kavun.enums;

/**
 * Form factor of the client device that issued a request.
 *
 * <p>Note: this describes the hardware, not the channel. A tablet using a web browser is
 * {@link #TABLET}; a desktop browser is {@link #DESKTOP}.
 */
public enum DeviceType {
    DESKTOP, TABLET, MOBILE, TV, BOT, UNKNOWN
}
