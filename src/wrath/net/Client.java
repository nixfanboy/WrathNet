/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net;

import java.io.File;
import java.io.Serializable;
import wrath.net.managers.ClientManager;
import wrath.net.managers.ClientRudpManager;
import wrath.net.managers.ClientTcpManager;
import wrath.net.managers.ClientUdpManager;
import wrath.util.Compression;
import wrath.util.Config;

/**
 * Class to represent the Client in a Client-Server infrastructure.
 * This class acts as a thin-layer for ease-of-use. All of the real work is done in the {@link wrath.net.managers.ClientManager} classes.
 * @author Trent Spears
 */
public class Client 
{
    private static final Config clientCfg = new Config(new File("netclient.cfg"));
    
    private ClientListener listener;
    private final ClientManager man;
    private final Protocol proto;
    
    /**
     * Constructor.
     * @param protocol The {@link wrath.net.Protocol} to use in the connection. This cannot be changed.
     * @param listener The {@link wrath.net.ClientListener} to report received data to.
     */
    public Client(Protocol protocol, ClientListener listener)
    {
        this.proto = protocol;
        this.listener = listener;
        
        if(proto == Protocol.TCP) man = new ClientTcpManager(this);
        else if(proto == Protocol.UDP) man = new ClientUdpManager(this);
        else man = new ClientRudpManager(this);
    }
    
    /**
     * Connects the client to the specified IP/hostname and port.
     * @see wrath.net.managers.ClientManager#connect(java.lang.String, int)
     * @param ip The IP Address or Hostname of the host to connect to.
     * @param port The port of the host.
     */
    public void connect(String ip, int port)
    {
        man.connect(ip, port);
    }
    
    /**
     * If data compression was enabled, this will disable it.
     * WARNING: If the Server has compression enabled then this Client will not be able to send/receive proper data from the Server.
     */
    public void disableDataCompression()
    {
        man.disableDataCompression();
    }
    
    /**
     * If data encryption was enabled, it is now disabled.
     * WARNING: If the Server has encryption enabled then this Client will not be able to send/receive proper data from the Server.
     */
    public void disableDataEncryption()
    {
        man.disableDataEncryption();
    }
    
    /**
     * Disconnects the client from any previously established connections.
     * @see wrath.net.managers.ClientManager#disconnect()
     */
    public void disconnect()
    {
        man.disconnect();
    }
    
    /**
     * Enables data being sent and received to be compressed and decompressed in specified format.
     * WARNING: This should only be enabled when sending large amounts of data.
     * @param format The format to compress the data with.
     */
    public void enableDataCompression(Compression.CompressionType format)
    {
        man.enableDataCompression(format);
    }
    
    /**
     * Enables all data going through this Client->Server connection to be encrypted/decrypted with the specified phrase/key.
     * @param passphrase The passphrase or key that must be at least 128-bit. No length limit below theoretical String length limit.
     * WARNING: The Server and Client must both have encryption enabled with the same passphrase/key.
     * WARNING: Enabling this process will slow the connection noticeably.
     */
    public void enableDataEncryption(String passphrase)
    {
        man.enableDataEncryption(passphrase);
    }
    
    /**
     * Gets the {@link wrath.util.Config} containing options for the Client.
     * @return Returns the {@link wrath.util.Config} containing options for the Client.
     */
    public static Config getClientConfig()
    {
        return clientCfg;
    }
    
    /**
     * Gets the {@link wrath.net.ClientListener} set to respond to received data.
     * @return Returns the {@link wrath.net.ClientListener} set to respond to received data.
     */
    public ClientListener getClientListener()
    {
        return listener;
    }
    
    /**
     * Gets the implementation layer object, the {@link wrath.net.managers.ClientManager}, of this Client.
     * @return Returns the implementation layer object, the {@link wrath.net.managers.ClientManager}, of this Client.
     */
    public ClientManager getClientManager()
    {
        return man;
    }
    
    /**
     * Gets the current state of the Client's connection, as represented by {@link wrath.net.ConnectionState}.
     * @see wrath.net.managers.ClientManager#getConnectionState() 
     * @return Returns the current state of the Client's connection, as represented by {@link wrath.net.ConnectionState}.
     */
    public ConnectionState getConnectionState()
    {
        return man.getConnectionState();
    }
    
    /**
     * Gets the IP Address/Hostname of the current or last Server in the form of a String.
     * Returns "0.0.0.0" if never connected.
     * @see wrath.net.managers.ClientManager#getServerIP() 
     * @return Returns the IP Address/Hostname of the Server in the form of a String.
     */
    public String getServerIP()
    {
        return man.getServerIP();
    }
    
    /**
     * Gets the port of the current or last Server.
     * Returns 0 if never connected.
     * @see wrath.net.managers.ClientManager#getServerPort() 
     * @return Returns the port of the Server.
     */
    public int getServerPort()
    {
        return man.getServerPort();
    }
    
    /**
     * Checks to see if the Client is currently connected to a host.
     * @see wrath.net.managers.ClientManager#isConnected() 
     * @return If true, the Client is currently connected to a Server. Otherwise false.
     */
    public boolean isConnected()
    {
        return man.isConnected();
    }
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @see wrath.net.managers.ClientManager#send(byte[]) 
     * @param data The raw byte data to send to the Server.
     */
    public void send(byte[] data)
    {
        man.send(data);
    }
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @see wrath.net.managers.ClientManager#send(java.io.Serializable)
     * @param object The data to send to the Server.
     */
    public void send(Serializable object)
    {
        man.send(object);
    }
    
    /**
     * Sends data to the Server the Client is connected to, if it is connected.
     * @see wrath.net.managers.ClientManager#send(wrath.net.Packet)
     * @param packet The {@link wrath.net.Packet} containing the data to send to the Server.
     */
    public void send(Packet packet)
    {
        man.send(packet);
    }
    
    /**
     * Changes the {@link wrath.net.ClientListener} associates with this Client.
     * @param listener The {@link wrath.net.ClientListener} to report received data to.
     */
    public void setClientListener(ClientListener listener)
    {
        this.listener = listener;
    }
    
    @Override
    public String toString()
    {
        return "[" + man.getServerIP() + ":" + man.getServerPort() + "]";
    }
}