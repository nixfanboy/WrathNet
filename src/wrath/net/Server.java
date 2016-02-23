/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import wrath.net.managers.ServerManager;
import wrath.net.managers.ServerTcpManager;
import wrath.net.managers.ServerUdpManager;
import wrath.util.Config;

/**
 * Class to represent the Server in a Client-Server infrastructure.
 * This class acts as a thin-layer for ease-of-use. All of the real work is done in the {@link wrath.net.managers.ServerManager} classes.
 * @author Trent Spears
 */
public class Server 
{
    private static final Config serverCfg = new Config(new File("netserver.cfg"));
    
    private final HashSet<SessionFlag> flags = new HashSet<>();
    private ServerListener listener;
    private final ServerManager man;
    private final Protocol proto;
    
    /**
     * Constructor.
     * @param protocol The {@link wrath.net.Protocol} the server should use for communications.
     * @param listener The {@link wrath.net.ServerListener} to report received data to.
     */
    public Server(Protocol protocol, ServerListener listener)
    {
        this(protocol, listener, new SessionFlag[0]);
    }
    
    /**
     * Constructor.
     * @param protocol The {@link wrath.net.Protocol} the server should use for communications.
     * @param listener The {@link wrath.net.ServerListener} to report received data to.
     * @param flags List of {@link wrath.net.SessionFlag}s to change the way the connection is established.
     */
    public Server(Protocol protocol, ServerListener listener, SessionFlag[] flags)
    {
        this(protocol, listener, Arrays.asList(flags));
    }
    
    /**
     * Constructor.
     * @param protocol The {@link wrath.net.Protocol} the server should use for communications.
     * @param listener The {@link wrath.net.ServerListener} to report received data to.
     * @param flags List of {@link wrath.net.SessionFlag}s to change the way the connection is established.
     */
    public Server(Protocol protocol, ServerListener listener, Collection<SessionFlag> flags)
    {
        this.proto = protocol;
        this.listener = listener;
        this.flags.addAll(flags);
        
        if(proto == Protocol.TCP) man = new ServerTcpManager(this);
        else man = new ServerUdpManager(this);
    }
    
    /**
     * Binds the server socket using the specified parameters.
     * @see wrath.net.managers.ServerManager#bindSocket(int) 
     * @param port The port for the server to listen to.
     */
    public void bindSocket(int port)
    {
        man.bindSocket(port);
    }
    
    /**
     * Binds the server socket using the specified parameters.
     * @see wrath.net.managers.ServerManager#bindSocket(java.lang.String, int) 
     * @param ip The IP address to listen on.
     * @param port The port for the server to listen to.
     */
    public void bindSocket(String ip, int port)
    {
        man.bindSocket(ip, port);
    }
    
    /**
     * Disconnects a client from the Server.
     * @see wrath.net.managers.ServerManager#disconnectClient(wrath.net.ServerClient) 
     * @param client The {@link wrath.net.ServerClient} to disconnect from the server.
     */
    public void disconnectClient(ServerClient client)
    {
        man.disconnectClient(client);
    }
    
    /**
     * Gets the list of {@link wrath.net.ServerClient}s connected to this Server.
     * @see wrath.net.managers.ServerManager#getClients() 
     * @return Returns the list of {@link wrath.net.ServerClient}s connected to this Server in the form of a {@link java.util.Collection}.
     */
    public Collection<ServerClient> getClients()
    {
        return man.getClients();
    }
    
    /**
     * Gets the current state of the Server's connection, as represented by {@link wrath.net.ConnectionState}.
     * @see wrath.net.managers.ServerManager#getConnectionState() 
     * @return Returns the current state of the Server's connection, as represented by {@link wrath.net.ConnectionState}.
     */
    public ConnectionState getConnectionState()
    {
        return man.getConnectionState();
    }
    
    /**
     * Gets the IP Address/Hostname of the current or last bound ServerSocket in the form of a String.
     * Returns null if never bound or no IP specified.
     * @see wrath.net.managers.ServerManager#getIP() 
     * @return Returns the IP Address/Hostname of the ServerSocket in the form of a String.
     */
    public String getIP()
    {
        return man.getIP();
    }
    
    /**
     * Gets the number of {@link wrath.net.ServerClient}s connected to the Server.
     * @see java.util.Collection#size() 
     * @return Returns the number of {@link wrath.net.ServerClient}s connected to the Server.
     */
    public int getNumberOfConnectedClients()
    {
        return man.getClients().size();
    }
    
    /**
     * Gets the port of the current or last bound ServerSocket.
     * Returns 0 if never bound.
     * @see wrath.net.managers.ServerManager#getPort() 
     * @return Returns the port of the ServerSocket.
     */
    public int getPort()
    {
        return man.getPort();
    }
    
    /**
     * Gets the {@link wrath.util.Config} containing options for the Server.
     * @return Returns the {@link wrath.util.Config} containing options for the Server.
     */
    public static Config getServerConfig()
    {
        return serverCfg;
    }
    
    /**
     * Gets the list of {@link wrath.net.SessionFlag}s that change the way the connection is established.
     * @return Returns the list of {@link wrath.net.SessionFlag}s that change the way the connection is established.
     */
    public Collection<SessionFlag> getSessionFlags()
    {
        return flags;
    }
    
    /**
     * Gets the {@link wrath.net.ServerListener} set to respond to received data.
     * @return Returns the {@link wrath.net.ServerListener} set to respond to received data.
     */
    public ServerListener getServerListener()
    {
        return listener;
    }
    
    /**
     * Checks to see if the Server is currently bound to a port.
     * @see wrath.net.managers.ServerManager#isBound() 
     * @return If true, the Server is currently bound to a port. Otherwise false.
     */
    public boolean isBound()
    {
        return man.isBound();
    }
    
    /**
     * Checks if a the specified {@link wrath.net.ServerClient} is connected to this Server.
     * @see wrath.net.managers.ServerManager#isClientConnected(wrath.net.ServerClient) 
     * @param client The {@link wrath.net.ServerClient} to check the connection state of.
     * @return Returns true if specified {@link wrath.net.ServerClient} is connected to this Server. Otherwise false.
     */
    public boolean isClientConnected(ServerClient client)
    {
        return man.isClientConnected(client);
    }
    
    /**
     * Sends data to the Client, if it is connected.
     * @see wrath.net.managers.ServerManager#send(wrath.net.ServerClient, wrath.net.Packet) 
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param data The {@link wrath.net.Packet} containing the data to send to the Client.
     */
    public void send(ServerClient client, byte[] data)
    {
        man.send(client, data);
    }
    
    /**
     * Sends data to the specified {@link wrath.net.ServerClient}, if it is connected.
     * @see wrath.net.managers.ServerManager#send(wrath.net.ServerClient, java.io.Serializable) 
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param object The object to send to the Client.
     */
    public void send(ServerClient client, Serializable object)
    {
        man.send(client, object);
    }
    
    /**
     * Sends data to the Client, if it is connected.
     * @see wrath.net.managers.ServerManager#send(wrath.net.ServerClient, wrath.net.Packet) 
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param packet The {@link wrath.net.Packet} containing the data to send to the Client.
     */
    public void send(ServerClient client, Packet packet)
    {
        man.send(client, packet);
    }
    
    /**
     * Changes the {@link wrath.net.ServerListener} associates with this Server.
     * @param listener The {@link wrath.net.ServerListener} to report received data to.
     */
    public void setServerListener(ServerListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Unbinds the Server socket from the previously specified port.
     * @see wrath.net.managers.ServerManager#unbindSocket() 
     * Also cleans up all resources associated with the Server connection.
     */
    public void unbindSocket()
    {
        man.unbindSocket();
    }
    
    @Override
    public String toString()
    {
        return "[" + getIP() + ":" + getPort() + "]";
    }
}
