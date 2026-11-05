package com.vyrriox.lauramod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identity of the mod for the code that does not depend on Minecraft or a loader: its id, its
 * display name and its logger. Shared by every target, so it never needs a port.
 *
 * @author vyrriox
 */
public final class LauraCore {
    public static final String MODID = "lauramod";
    public static final String NAME = "My Girlfriend Laura";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private LauraCore() {
    }
}
