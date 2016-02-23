/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
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
     * Reliable User Datagram Protocol (UNDER CONSTRUCTION, DEFUNCT).
     * Built off of, and backwards compatible with, User Datagram Protocol.
     * Same as UDP, but ensures that received packets are in order.
     * @see wrath.net.Protocol#UDP
     */
    RUDP,
    /**
     * User Datagram Protocol.
     * @see java.net.DatagramPacket
     * @see java.net.DatagramSocket
     */
    UDP;
}
