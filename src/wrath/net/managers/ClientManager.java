/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
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
import wrath.util.MiscUtils;

/**
 * Abstract class that allows for polymorphism based on the protocol used in a connection.
 * @author Trent Spears
 */
public abstract class ClientManager
{
    protected Client client;
    protected String ip = "0.0.0.0";
    protected int port = 0;
    protected volatile boolean recvFlag = false;
    private final ArrayList<ReceivedEvent> execList = new ArrayList<>();
    protected Thread execThread;
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
     * Connects the Client to a Server using the {@link wrath.net.Protocol} specified in the Constructor.
     * @param ip The {@link java.lang.String} representation of the server's IP Address or Host Name.
     * @param port The port the server is listening on.
     */
    public synchronized void connect(String ip, int port)
    {
        if(isConnected()) return;
        
        this.ip = ip;
        this.port = port;
        
        state = ConnectionState.CONNECTING;
        System.out.println("] Connecting to [" + ip + ":" + port + "]!");
        
        execThread = new Thread(() ->
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
                        if(client.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION) && MiscUtils.isGZIPCompressed(p.getRawData()))  p = new Packet(MiscUtils.decompressData(p.getRawData(), MiscUtils.CompressionType.GZIP));
                        else if(Client.getClientConfig().getBoolean("CheckForGZIPCompression", false)) if(MiscUtils.isGZIPCompressed(p.getRawData())) p = new Packet(MiscUtils.decompressData(p.getRawData(), MiscUtils.CompressionType.GZIP));
                        if(p.getDataAsObject().equals(Packet.TERMINATION_CALL)) disconnect(false);
                        else event.client.getClientListener().onReceive(event.client, p);
                    }
                }
        });
        
        try
        {
            createNewSocket(new InetSocketAddress(InetAddress.getByName(ip), port));
            
            recvFlag = false;
            recvThread.setName("NetClientRecvThread");
            recvThread.setDaemon(true);
            recvThread.start();
            execThread.setName("NetClientExecThread");
            execThread.setDaemon(true);
            execThread.start();
            
            state = ConnectionState.CONNECTED;
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
    protected abstract void disconnect(boolean calledFirst);
    
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
     * Sends data to the Server the Client is connected to, if it is connected.
     * @param data The raw byte data to send to the Server.
     */
    public abstract void send(byte[] data);
    
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
