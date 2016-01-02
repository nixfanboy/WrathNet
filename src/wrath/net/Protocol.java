/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net;

/**
 * Enum containing the possible protocols in a connection.
 * @author Trent Spears
 */
public enum Protocol 
{
    /**
     * Transmission Control Protocol.
     * @see java.net.Socket
     * @see java.net.ServerSocket
     */
    TCP,
    /**
     * User Datagram Protocol.
     * @see java.net.DatagramPacket
     * @see java.net.DatagramSocket
     */
    UDP;
}
