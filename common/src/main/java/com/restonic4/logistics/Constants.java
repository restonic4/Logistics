package com.restonic4.logistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    public static final String MOD_ID = "logistics";
    public static final String MOD_NAME = "Logistics";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // This MUST be a power of 2, since BFS pathfinding needs it to be.
    public static final int MAX_NODES_PER_NETWORK = 1 << 16; // 65,536
}
