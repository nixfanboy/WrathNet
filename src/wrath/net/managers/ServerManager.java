/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import javax.crypto.spec.SecretKeySpec;
import wrath.net.ConnectionState;
import wrath.net.Packet;
import wrath.net.Server;
import wrath.net.ServerClient;
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
    private Compression.CompressionType compressFormat = null;
    private SecretKeySpec encryptKey = null;
    protected int port = 0;
    protected volatile boolean recvFlag = false;
    protected Thread recvThread;
    protected Server server;
    protected ConnectionState state = ConnectionState.SOCKET_NOT_BOUND;
    
    private final ArrayList<ServerClient> conList = new ArrayList<>();
    private final ArrayList<ServerClient> dconList = new ArrayList<>();
    private final ArrayList<ServerReceivedEvent> execList = new ArrayList<>();

    /**
     * Thread where all data is processed. This includes compression, encryption, and the onReceive(), onClientConnect(), and onClientDisconnect() methods.
     */
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
                    if(encryptKey != null) p = new Packet(Encryption.decryptData(p.getRawData(), encryptKey));
                    
                    // Decompress
                    if(compressFormat != null) p = new Packet(Compression.decompressData(p.getRawData(), compressFormat));
                    
                    // Check if TERMINATION_CALL packet. Pushes event to Listener if not.
                    try
                    {
                        if(Arrays.equals(p.getRawData(), Packet.TERMINATION_CALL)) disconnectClient(event.client, false);
                        else event.client.getServer().getServerListener().onReceive(event.client, p);
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
        
        // Set State
        state = ConnectionState.BINDING_PORT;
        System.out.println("] Binding ServerSocket to [" + ip + ":" + port + "]!");
        
        try
        {
            // Create the Socket
            createSocket(ip, port);
            
            // Reset Flag
            recvFlag = false;
            // Manage Threads
            recvThread.setName("NetServerRecvThread");
            execThread.setName("NetServerExecThread");
            execThread.setDaemon(true);
            execThread.start();
            recvThread.start();
            
            // Set State
            state = ConnectionState.LISTENING;
            System.out.println("] ServerSocket Bound to [" + ip + ":" + port + "]!");
        }
        catch(IOException e)
        {
            System.err.println("] Could not bind ServerSocket to [" + ip + ":" + port + "]! UNKNOWN Error!");
            state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
        }
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
     * @throws java.io.IOException
     */
    protected abstract void createSocket(String ip, int port) throws IOException;
    
    /**
     * If data compression was enabled, this will disable it.
     * WARNING: If a Client has compression enabled then the Client will not be able to send/receive proper data from/to this Server.
     */
    public void disableDataCompression()
    {
        compressFormat = null;
    }
    
    /**
     * If data encryption was enabled, this will disable it.
     * WARNING: If a Client has encryption enabled then this Server will not be able to send/receive proper data from that Client.
     */
    public void disableDataEncryption()
    {
        encryptKey = null;
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
            client.send(Packet.TERMINATION_CALL);
        }
        else System.out.println("] Client " + client.getClientIdentifier() + " Disconnecting.");
        clients.remove(client);
        removeClient(client);
        
        onClientDisconnect(client);
        System.out.println("] Client " + client.getClientIdentifier() + " Disconnected. ConnectionTime: " + ((double)(System.nanoTime() - client.getJoinTime())/1000000000) + "s");
    }
    
    /**
     * Enables data being sent and received to be compressed and decompressed in specified format.
     * WARNING: This should only be enabled when sending large amounts of data.
     * WARNING: The Server and Client must both have compression enabled with the same format.
     * @param format The format to compress the data with.
     */
    public void enableDataCompression(Compression.CompressionType format)
    {
        compressFormat = format;
    }
    
    /**
     * Enables all data going through this Server->Client connection to be encrypted/decrypted with the specified phrase/key.
     * @param passphrase The passphrase or key that must be at least 128-bit. No length limit below theoretical String length limit.
     * WARNING: The Client and Server must both have encryption enabled with the same passphrase/key.
     * WARNING: Enabling this process will slow the connection noticeably.
     */
    public void enableDataEncryption(String passphrase)
    {
        encryptKey = Encryption.generateKey(passphrase, "salt");
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
     * Pushes the data through the socket through the implementation class.
     * @param client The {@link wrath.net.ServerClient} to send data to.
     * @param data The final data to be sent, after compression and encryption.
     */
    protected abstract void pushData(ServerClient client, byte[] data);
    
    /**
     * Called when a packet is received and then placed into a queue that will later get executed on the execution thread.
     * @param c The {@link wrath.net.ServerClient} being managed.
     * @param p The {@link wrath.net.Packet} containing the received data.
     */
    protected void receive(ServerClient c, Packet p)
    {
        execList.add(new ServerReceivedEvent(c, p));
    }
    
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
            if(compressFormat != null) data = Compression.compressData(data, compressFormat);
            
            // Encryption
            if(encryptKey != null) data = Encryption.encryptData(data, encryptKey);
            
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
    
    private class ServerReceivedEvent
    {
        public final ServerClient client;
        public final Packet packet;

        private ServerReceivedEvent(ServerClient c, Packet p)
        {
            this.client = c;
            this.packet = p;
        }
    }
}