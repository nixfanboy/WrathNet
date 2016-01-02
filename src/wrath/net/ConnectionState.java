/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net;

/**
 * Enum describing the current state of a connection.
 * @author Trent Spears
 */
public enum ConnectionState 
{
    /**
     * When a Server or UDP-based Client is currently binding to a port.
     */
    BINDING_PORT,
    /**
     * When a TCP-based Client is currently setting up the connection.
     */
    CONNECTING,
    /**
     * When a TCP-based Client is successfully connected to a Server.
     */
    CONNECTED,
    /**
     * When a Client unexpectedly loses connection from the server.
     */
    DISCONNECTED_CONNECTION_DROPPED,
    /**
     * When a Client fails to connect to a specified host due to an error.
     */
    DISCONNECTED_CONNECTION_FAILED,
    /**
     * When the Client has been generated but has not been connected to a server. This is the default state of a Client.
     */
    DISCONNECTED_IDLE,
    /**
     * When the Client or Server has properly ended the session.
     */
    DISCONNECTED_SESSION_CLOSED,
    /**
     * When a Server or UDP-based Client is awaiting data.
     */
    LISTENING,
    /**
     * When a Server or UDP-based Client has closed its listening socket.
     */
    SOCKET_CLOSED,
    /**
     * When a Server or UDP-based Client has been generated but not bound to a socket. This is the default state of a Server.
     */
    SOCKET_NOT_BOUND,
    /**
     * When a Server or UDP-based Client failed to bind its socket to a port.
     */
    SOCKET_NOT_BOUND_ERROR;
}
