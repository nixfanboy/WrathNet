/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net;

/**
 * Enum containing all of the possible flags for any connection.
 * @author Trent Spears
 */
public enum SessionFlag
{
    /**
     * Enables the use of network compression using the GZIP format.
     * It is only recommended to use this if Packets are being sent with large amounts of data at a time.
     * Enabling this flag on a Server requires all Clients to also have this flag enabled.
     * This can be used for any protocol.
     */
    GZIP_COMPRESSION,
    /**
     * Enables multi-casting for UDP Servers. Must bind to a specified multi-cast IP Address.
     * Clients will connect to the multi-casting IP instead of host's IP Address.
     * Only Servers using the UDP protocol may enable this flag.
     */
    MULTICAST,
    /**
     * Enables the use of Secure Socket Layers for TCP Connections.
     * This will slow down the connection severely, but also encrypt the messages being sent/received.
     * Enabling this flag on a Server requires all Clients to also have this flag enabled.
     * This can only be used with the TCP protocol.
     */
    USE_SSL;
}
