/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net.managers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import wrath.net.ConnectionState;
import wrath.net.Packet;
import wrath.net.Server;
import wrath.net.ServerClient;

/**
 * Abstract class that allows for polymorphism based on the protocol used in a connection.
 * @author Trent Spears
 */
public abstract class ServerManager
{
    protected Thread clientRecvThread;
    protected final HashSet<ServerClient> clients = new HashSet<>();
    protected String ip = null;
    protected int port = 0;
    protected volatile boolean recvFlag = false;
    protected Thread recvThread;
    protected final Server server;
    protected ConnectionState state = ConnectionState.SOCKET_NOT_BOUND;
    
    private final ArrayList<ServerReceivedEvent> execList = new ArrayList<>();
    protected final Thread execThread = new Thread(() ->
    {
        while(!recvFlag)
            if(!execList.isEmpty())
            {
                ServerReceivedEvent[] events = new ServerReceivedEvent[execList.size()];
                execList.toArray(events);
                execList.clear();
                for(ServerReceivedEvent event : events) event.client.getServer().getServerListener().onReceive(event.client, event.packet);
            }
    });
    
    /**
     * Constructor.
     * @param server The {@link wrath.net.Server} that is being managed. 
     */
    protected ServerManager(Server server)
    {
        this.server = server;
    }

    /**
     * Binds the server socket using the specified parameters.
     * @param ip The IP address to listen on.
     * @param port The port for the server to listen to.
     */
    public abstract void bindSocket(String ip, int port);
    
    /**
     * Binds the server socket using the specified parameters.
     * @param port The port for the server to listen to.
     */
    public void bindSocket(int port)
    {
        bindSocket(null, port);
    }
    
    /**
     * Disconnects a client from the Server.
     * @param client The {@link wrath.net.ServerClient} to disconnect from the server.
     */
    public void disconnectClient(ServerClient client)
    {
        disconnectClient(client, true);
    }
    
    /**
     * Disconnects a client from the Server.
     * @param client The {@link wrath.net.ServerClient} to disconnect from the server.
     * @param calledFirst If true, then the Server is disconnecting from the Client. If false, the Client is disconnecting from the Server.
     */
    public abstract void disconnectClient(ServerClient client, boolean calledFirst);
    
    /**
     * Gets the list of {@link wrath.net.ServerClient}s connected to this Server.
     * @return Returns the list of {@link wrath.net.ServerClient}s connected to this Server in the form of a {@link java.util.Collection}.
     */
    public Collection<ServerClient> getClients()
    {
        return clients;
    }
    
    /**
     * Gets the current state of the Server's connection, as represented by {@link wrath.net.ConnectionState}.
     * @return Returns the current state of the Server's connection, as represented by {@link wrath.net.ConnectionState}.
     */
    public ConnectionState getConnectionState()
    {
        return state;
    }
    
    /**
     * Gets the IP Address/Hostname of the current or last bound ServerSocket in the form of a String.
     * Returns null if never bound or no IP specified.
     * @see wrath.net.managers.ServerManager#getIP() 
     * @return Returns the IP Address/Hostname of the ServerSocket in the form of a String.
     */
    public String getIP()
    {
        return ip;
    }
    
    /**
     * Gets the port of the current or last bound ServerSocket.
     * Returns 0 if never bound.
     * @see wrath.net.managers.ServerManager#getPort() 
     * @return Returns the port of the ServerSocket.
     */
    public int getPort()
    {
        return port;
    }
    
    /**
     * Checks to see if the Server is currently bound to a port.
     * @see wrath.net.managers.ServerManager#isBound() 
     * @return If true, the Server is currently bound to a port. Otherwise false.
     */
    public abstract boolean isBound();
    
    /**
     * Checks if a the specified {@link wrath.net.ServerClient} is connected to this Server.
     * @param client The {@link wrath.net.ServerClient} to check the connection state of.
     * @return Returns true if specified {@link wrath.net.ServerClient} is connected to this Server. Otherwise false.
     */
    public boolean isClientConnected(ServerClient client)
    {
        return clients.contains(client);
    }
    
    /**
     * Called when a packet is received and then placed into a queue that will later get executed on the execution thread.
     * @param c The {@link wrath.net.ServerClient} being managed.
     * @param p The {@link wrath.net.Packet} containing the received data.
     */
    protected void onReceive(ServerClient c, Packet p)
    {
        execList.add(new ServerReceivedEvent(c, p));
    }
    
    /**
     * Sends data to the specified {@link wrath.net.ServerClient}, if it is connected.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param object The object to send to the Client.
     */
    public void send(ServerClient client, Serializable object)
    {
        send(client, new Packet(object).getRawData());
    }
    
    /**
     * Sends data to the specified {@link wrath.net.ServerClient}, if it is connected.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param packet The {@link wrath.net.Packet} containing the data to send to the Client.
     */
    public void send(ServerClient client, Packet packet)
    {
        send(client, packet.getRawData());
    }
    
    /**
     * Sends data to the specified {@link wrath.net.ServerClient}, if it is connected.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param data The data to send to the Client.
     */
    public abstract void send(ServerClient client, byte[] data);
    
    /**
     * Unbinds the Server socket from the previously specified port.
     * Also cleans up all resources associated with the Server connection.
     */
    public abstract void unbindSocket();
}

class ServerReceivedEvent
{
    public final ServerClient client;
    public final Packet packet;

    protected ServerReceivedEvent(ServerClient c, Packet p)
    {
        this.client = c;
        this.packet = p;
    }
}