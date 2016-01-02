/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net;

import java.io.Serializable;

/**
 * Class to represent a Client that is connected to a Server.
 * @author Trent Spears
 */
public class ServerClient
{
    private String ident = "";
    private final long joinTime = System.nanoTime();
    private final Server server;
    
    /**
     * Constructor.
     * @param server The local {@link wrath.net.Server} that this Client is connected to. 
     * @param identifier The wanted {@link java.lang.String} identifier for the Client. Typically "[IP:PORT]".
     */
    public ServerClient(Server server, String identifier)
    {
        this.server = server;
        this.ident = identifier;
    }
    
    /**
     * Calls for the client to disconnect from the server.
     * If the client does not disconnect first then the connection will be forcibly closed.
     */
    public void disconnectClient()
    {
        server.disconnectClient(this);
    }
    
    /**
     * Gets the {@link java.lang.String} identifier for the Client. Typically "[IP:PORT]".
     * @return Returns the {@link java.lang.String} identifier for the Client.
     */
    public String getClientIdentifier()
    {
        return ident;
    }
    
    /**
     * Gets the Unix-format timestamp for when the Client connected to the server.
     * @return Returns the Unix-format timestamp for when the Client connected to the server.
     */
    public long getJoinTime()
    {
        return joinTime;
    }
    
    /**
     * Gets the local {@link wrath.net.Server} that this Client is connected to. 
     * @return Returns the local {@link wrath.net.Server} that this Client is connected to.
     */
    public Server getServer()
    {
        return server;
    }
    
    /**
     * Gets if the Client is still connected to this Server.
     * @return If true, the Client is still connected to this Server. Otherwise false.
     */
    public boolean isConnected()
    {
        return server.isClientConnected(this);
    }
    
    /**
     * Sends data to the Client.
     * @param packet The data to send to the client in the form of a {@link wrath.net.Packet}.
     */
    public void send(Packet packet)
    {
        server.send(this, packet);
    }
    
    /**
     * Sends data to the Client.
     * @param object The data to send to the client in the form of any {@link java.io.Serializable} object.
     */
    public void send(Serializable object)
    {
        server.send(this, object);
    }
    
    /**
     * Sends data to the Client.
     * @param data The raw byte data to send to the client.
     */
    public void send(byte[] data)
    {
        server.send(this, data);
    }
    
    /**
     * Changes the {@link java.lang.String} identifier for the Client.
     * @param identifier The wanted {@link java.lang.String} identifier for the Client. Typically "[IP:PORT]".
     */
    public void setClientIdentifier(String identifier)
    {
        this.ident = identifier;
    }
    
    @Override
    public String toString()
    {
        return ident;
    }
}
