/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
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
import wrath.net.SessionFlag;
import wrath.util.Compression;
import wrath.util.Encryption;

/**
 * Abstract class that allows for polymorphism based on the protocol used in a connection.
 * @author Trent Spears
 */
public abstract class ServerManager
{
    protected Thread clientRecvThread;
    protected final HashSet<ServerClient> clients = new HashSet<>();
    protected String ip = null;
    private String encryptKey = "";
    protected int port = 0;
    protected volatile boolean recvFlag = false;
    protected Thread recvThread;
    protected Server server;
    protected ConnectionState state = ConnectionState.SOCKET_NOT_BOUND;
    
    private final ArrayList<ServerClient> conList = new ArrayList<>();
    private final ArrayList<ServerClient> dconList = new ArrayList<>();
    private final ArrayList<ServerReceivedEvent> execList = new ArrayList<>();
    protected final Thread execThread = new Thread(() ->
    {
        while(!recvFlag)
        {
            if(!conList.isEmpty())
            {
                ServerClient[] clis = new ServerClient[conList.size()];
                conList.toArray(clis);
                conList.clear();
                for(ServerClient c : clis) c.getServer().getServerListener().onClientConnect(c);
            }
            
            if(!dconList.isEmpty())
            {
                ServerClient[] clis = new ServerClient[dconList.size()];
                dconList.toArray(clis);
                dconList.clear();
                for(ServerClient c : clis) c.getServer().getServerListener().onClientDisconnect(c);
            }
            
            if(!execList.isEmpty())
            {
                ServerReceivedEvent[] events = new ServerReceivedEvent[execList.size()];
                execList.toArray(events);
                execList.clear();
                for(ServerReceivedEvent event : events)
                {
                    Packet p = event.packet;
                    // Decrypt
                    if(!encryptKey.equals("")) p = new Packet(Encryption.decryptData(p.getRawData(), encryptKey));
                    
                    // Decompress
                    if(server.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION) || (Server.getServerConfig().getBoolean("CheckForGZIPCompression", false) && Compression.isGZIPCompressed(p.getRawData())))
                        p = new Packet(Compression.decompressData(p.getRawData(), Compression.CompressionType.GZIP));
                    
                    // Check if TERMINATION_CALL packet. Pushes event to Listener if not.
                    try
                    {
                        if(new String(p.getRawData()).equals(Packet.TERMINATION_CALL)) disconnectClient(event.client, false);
                        else
                        {
                            if(p == null) p = event.packet;
                            event.client.getServer().getServerListener().onReceive(event.client, p);
                        }
                    }
                    catch(NullPointerException e){}
                }
            }
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
    public void bindSocket(String ip, int port)
    {
        // Check if Bound
        if(isBound()) return;
        
        // Set IP and Port
        if(ip == null) ip = "*";
        this.ip = ip;
        this.port = port;
        
        // Reset Flag
        recvFlag = false;
        
        // Set State
        state = ConnectionState.BINDING_PORT;
        
        createSocket(ip, port);
        
        if(!isBound())
        {
            System.err.println("] Could not bind ServerSocket to [" + ip + ":" + port + "]! UNKNOWN Error!");
            state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
        }
        else state = ConnectionState.LISTENING;
        
        recvThread.start();
        execThread.start();
    }
    
    /**
     * Binds the server socket using the specified parameters.
     * @param port The port for the server to listen to.
     */
    public void bindSocket(int port)
    {
        bindSocket(null, port);
    }
    
    /**
     * Closes the implementation layer objects involved with the socket.
     */
    protected abstract void closeSocket();
    
    /**
     * Creates the implementation layer socket object.
     * @param ip The ip to bind to. Binds to '*' if NULL.
     * @param port The port to bind to.
     */
    protected abstract void createSocket(String ip, int port);
    
    /**
     * If data encryption was enabled, it is now disabled.
     * WARNING: If a Client has encryption enabled then this Server will not be able to send/receive proper data from that Client.
     */
    public void disableDataEncryption()
    {
        encryptKey = "";
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
    public void disconnectClient(ServerClient client, boolean calledFirst)
    {
        if(!clients.contains(client)) return;
        if(calledFirst)
        {
            System.out.println("] Disconnecting Client " + client.getClientIdentifier() + ".");
            client.send(Packet.TERMINATION_CALL.getBytes());
        }
        else System.out.println("] Client " + client.getClientIdentifier() + " Disconnecting.");
        clients.remove(client);
        removeClient(client);
        
        onClientDisconnect(client);
        System.out.println("] Client " + client.getClientIdentifier() + " Disconnected. ConnectionTime: " + ((double)(System.nanoTime() - client.getJoinTime())/1000000000) + "s");
    }
    
    /**
     * Enables all data going through this Server->Client connection to be encrypted/decrypted with the specified phrase/key.
     * @param passphrase The passphrase or key that must be at least 128-bit. No length limit below theoretical String length limit.
     * WARNING: The Client and Server must both have encryption enabled with the same passphrase/key.
     * WARNING: Enabling this process will slow the connection noticeably.
     */
    public void enableDataEncryption(String passphrase)
    {
        encryptKey = passphrase;
    }
    
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
     * Called when a {@link wrath.net.ServerClient} connects to the server.
     * @param c The {@link wrath.net.ServerClient} connecting to the server.
     */
    protected void onClientConnect(ServerClient c)
    {
        conList.add(c);
    }
    
    /**
     * Called when a {@link wrath.net.ServerClient} disconnects from the server.
     * @param c The {@link wrath.net.ServerClient} disconnecting from the server.
     */
    protected void onClientDisconnect(ServerClient c)
    {
        dconList.add(c);
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
                svr.send(new DatagramPacket(data, data.length, client.getAddress(), client.getPort()));
     * Pushes the data through the socket through the implementation class.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param data The final data to be sent, after compression and encryption.
     */
    protected abstract void pushData(ServerClient client, byte[] data);
    
    /**
     * Removes the {@link wrath.net.ServerClient} from any implementation-layer registries.
     * @param client The {@link wrath.net.ServerClient} that is disconnecting.
     */
    protected abstract void removeClient(ServerClient client);
    
    /**
     * Sends data to the specified {@link wrath.net.ServerClient}, if it is connected.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param data The data to send to the Client.
     */
    public void send(ServerClient client, byte[] data)
    {
        if(clients.contains(client))
        {
            // Compression
            if(server.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION)) data = Compression.compressData(data, Compression.CompressionType.GZIP);
                
            // Encryption
            if(!encryptKey.equals("")) data = Encryption.encryptData(data, encryptKey);
                
            // Push data
            pushData(client, data);
        }
        else System.out.println("] WARNING: Attempted to send data to unknown client!");
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
     * Sends data to the Client, if it is connected.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param packet The {@link wrath.net.Packet} containing the data to send to the Client.
     */
    public void send(ServerClient client, Packet packet)
    {
        send(client, packet.getRawData());
    }
    
    /**
     * Unbinds the Server socket from the previously specified port.
     * Also cleans up all resources associated with the Server connection.
     */
    public void unbindSocket()
    {
        if(!isBound()) return;
        System.out.println("] Closing ServerSocket.");
        
        ServerClient[] clis = new ServerClient[clients.size()];
        clients.toArray(clis);
        for(ServerClient c : clis)
            if(c.isConnected()) c.disconnectClient();
        clients.clear();
        
        recvFlag = true;
        
        closeSocket();
        
        state = ConnectionState.SOCKET_CLOSED;
        System.out.println("] ServerSocket Closed.");
    }
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