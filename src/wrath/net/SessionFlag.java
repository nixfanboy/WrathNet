/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
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
     * Both the Client and the Server must have this enabled for this to work.
     * This can be used for any protocol.
     */
    GZIP_COMPRESSION,
    /**
     * Enables the use of Secure Socket Layers for TCP Connections.
     * This will slow down the connection severely, but also encrypt the messages being sent/received.
     * Both the Client and the Server must have this enabled for this to work.
     * This can only be used with the TCP protocol.
     */
    USE_SSL;
}
