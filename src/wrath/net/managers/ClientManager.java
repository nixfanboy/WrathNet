/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import wrath.net.Client;
import wrath.net.ConnectionState;
import wrath.net.Packet;
import wrath.net.SessionFlag;
import wrath.util.Compression;
import wrath.util.Encryption;

/**
 * Abstract class that allows for polymorphism based on the protocol used in a connection.
 * @author Trent Spears
 */
public abstract class ClientManager
{
    protected Client client;
    protected String ip = "0.0.0.0";
    protected int port = 0;
    private String encryptKey = "";
    protected volatile boolean recvFlag = false;
    private final ArrayList<ReceivedEvent> execList = new ArrayList<>();
    protected Thread execThread = new Thread(() ->
        {
            while(!recvFlag)
                if(!execList.isEmpty())
                {
                    ReceivedEvent[] events = new ReceivedEvent[execList.size()];
                    execList.toArray(events);
                    execList.clear();
                    for(ReceivedEvent event : events)
                    {
                        Packet p = event.packet;
                        // Decrypt
                        if(!encryptKey.equals("")) p = new Packet(Encryption.decryptData(p.getRawData(), encryptKey));
                        
                        // Decompress
                        if(client.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION) || (Client.getClientConfig().getBoolean("CheckForGZIPCompression", false) && Compression.isGZIPCompressed(p.getRawData())))  
                            p = new Packet(Compression.decompressData(p.getRawData(), Compression.CompressionType.GZIP));
                        
                        // Check if TERMINATION_CALL packet. Pushes event to Listener if not.
                        try
                        {
                            if(new String(p.getRawData()).equals(Packet.TERMINATION_CALL)) disconnect(false);
                            else
                            {
                                if(p == null) p = event.packet;
                                event.client.getClientListener().onReceive(event.client, p);
                            }
                        }
                        catch(NullPointerException e) {}
                    }
                }
        });
    protected Thread recvThread;
    protected ConnectionState state = ConnectionState.DISCONNECTED_IDLE;
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} that is being managed. 
     */
    protected ClientManager(Client client)
    {
        this.client = client;
    }
    
    /**
     * Closes the socket objects in the implementation layer.
     */
    protected abstract void closeSocket();
    
    /**
     * Connects the Client to a Server using the {@link wrath.net.Protocol} specified in the Constructor.
     * @param ip The {@link java.lang.String} representation of the server's IP Address or Host Name.
     * @param port The port the server is listening on.
     */
    public synchronized void connect(String ip, int port)
    {
        // Check if connected
        if(isConnected()) return;
        
        // Set IP and Port to track connection
        this.ip = ip;
        this.port = port;
        
        // Set State
        state = ConnectionState.CONNECTING;
        System.out.println("] Connecting to [" + ip + ":" + port + "]!");
        
        try
        {
            // Create the Socket
            createNewSocket(new InetSocketAddress(InetAddress.getByName(ip), port));
            
            // Reset Flag
            recvFlag = false;
            // Manage Threads
            recvThread.setName("NetClientRecvThread");
            recvThread.setDaemon(true);
            recvThread.start();
            execThread.setName("NetClientExecThread");
            execThread.setDaemon(true);
            execThread.start();
            
            // Set State
            state = ConnectionState.CONNECTED;
            client.getClientListener().onConnect(client);
            System.out.println("] Connected to [" + ip + ":" + port + "]!");
        }
        catch(UnknownHostException e)
        {
            System.err.println("] Could not resolve hostname/ip [" + ip + "]!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
        catch(IOException e)
        {
            System.err.println("] Could not connect to [" + ip + ":" + port + "]! I/O Error!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
    }
    
    /**
     * Method used by Client Managers to create a new Socket and Receiving Thread.
     * @param address The {@link java.net.InetSocketAddress} representation of the Server the Client is connecting to.
     * @throws java.io.IOException
     */
    protected abstract void createNewSocket(InetSocketAddress address) throws IOException;
    
    /**
     * If data encryption was enabled, it is now disabled.
     * WARNING: If the Server has encryption enabled then this Client will not be able to send/receive proper data from the Server.
     */
    public void disableDataEncryption()
    {
        encryptKey = "";
    }
    
    /**
     * Disconnects from the Server, if the Client is connected.
     */
    public synchronized void disconnect()
    {
        disconnect(true);
    }
    
    /**
     * Disconnects from the Server, if the Client is connected.
     * @param calledFirst If true, then the Client is disconnecting from the Server. If false, the Server is disconnecting from the Client.
     */
    public synchronized void disconnect(boolean calledFirst)
    {
        // Check if still connected
        if(!isConnected()) return;
        // Signal other threads to stop
        recvFlag = true;
        // Check for the disconnect signal if Server is dropping this client. Otherwise send disconnect signal to Server.
        if(!calledFirst) System.out.println("] Received disconnect signal from host.");
        else send(Packet.TERMINATION_CALL.getBytes());
        System.out.println("] Disconnecting from [" + ip + ":" + port + "]!");
        
        closeSocket();
        
        if(state == ConnectionState.CONNECTED) state = ConnectionState.DISCONNECTED_SESSION_CLOSED;
        client.getClientListener().onDisconnect(client);
        System.out.println("] Disconnected.");
    }
    
    /**
     * Enables all data going through this Client->Server connection to be encrypted/decrypted with the specified phrase/key.
     * @param passphrase The passphrase or key that must be at least 128-bit. No length limit below theoretical String length limit.
     * WARNING: The Server and Client must both have encryption enabled with the same passphrase/key.
     * WARNING: Enabling this process will slow the connection noticeably.
     */
    public void enableDataEncryption(String passphrase)
    {
        encryptKey = passphrase;
    }
    
    /**
     * Gets the current state of the Connection, as represented by a value from {@link wrath.net.ConnectionState}.
     * @return Returns the current state of the Connection, as represented by a value from {@link wrath.net.ConnectionState}.
     */
    public ConnectionState getConnectionState()
    {
        return state;
    }
    
    /**
     * Gets the IP Address/Hostname of the current or last Server in the form of a String.
     * Returns "0.0.0.0" if never connected.
     * @return Returns the IP Address/Hostname of the Server in the form of a String.
     */
    public String getServerIP()
    {
        return ip;
    }
    
    /**
     * Gets the port of the current or last Server.
     * Returns 0 if never connected.
     * @return Returns the port of the Server.
     */
    public int getServerPort()
    {
        return port;
    }
    
    /**
     * Checks to see if the Client is currently connected to a host.
     * @return If true, the Client is currently connected to a Server. Otherwise false.
     */
    public abstract boolean isConnected();
    
    /**
     * Called when a packet is received and then placed into a queue that will later get executed on the execution thread.
     * @param c The {@link wrath.net.Client} being managed.
     * @param p The {@link wrath.net.Packet} containing the received data.
     */
    protected void onReceive(Client c, Packet p)
    {
        execList.add(new ReceivedEvent(c, p));
    }
    
    /**
     * Sends the final data to the Server.
     * @param data The final data to send after compression and encryption.
     */
    protected abstract void pushData(byte[] data);
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @param data The raw byte data to send to the Server.
     */
    public void send(byte[] data)
    {
        if(client.isConnected())
        {
            // Compression
            if(client.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION)) data = Compression.compressData(data, Compression.CompressionType.GZIP);
            // Encryption
            if(!encryptKey.equals("")) data = Encryption.encryptData(data, encryptKey);
            
            pushData(data);
        }
    }
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @param object The data to send to the Server.
     */
    public void send(Serializable object)
    {
        send(new Packet(object).getRawData());
    }
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @param packet The {@link wrath.net.Packet} containing the data to send to the Server.
     */
    public void send(Packet packet)
    {
        send(packet.getRawData());
    }
}

class ReceivedEvent
{
    public final Client client;
    public final Packet packet;

    protected ReceivedEvent(Client c, Packet p)
    {
        this.client = c;
        this.packet = p;
    }
}
