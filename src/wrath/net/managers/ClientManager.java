/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net.managers;

import java.io.Serializable;
import java.util.ArrayList;
import wrath.net.Client;
import wrath.net.ConnectionState;
import wrath.net.Packet;

/**
 * Abstract class that allows for polymorphism based on the protocol used in a connection.
 * @author Trent Spears
 */
public abstract class ClientManager
{
    protected final Client client;
    protected String ip = "0.0.0.0";
    protected int ping = -1;
    protected int port = 0;
    protected volatile boolean recvFlag = false;
    private final ArrayList<ReceivedEvent> execList = new ArrayList<>();
    protected final Thread execThread = new Thread(() ->
    {
        while(!recvFlag)
            if(!execList.isEmpty())
            {
                ReceivedEvent[] events = new ReceivedEvent[execList.size()];
                execList.toArray(events);
                execList.clear();
                for(ReceivedEvent event : events)
                {
                    if(event.packet.getDataAsObject().equals(Packet.TERMINATION_CALL)) disconnect(false);
                    event.client.getClientListener().onReceive(event.client, event.packet);
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
     * Connects the Client to a Server using the {@link wrath.net.Protocol} specified in the Constructor.
     * @param ip The {@link java.lang.String} representation of the server's IP Address or Host Name.
     * @param port The port the server is listening on.
     */
    public abstract void connect(String ip, int port);
    
    /**
     * Disconnects from the Server, if the Client is connected.
     */
    public void disconnect()
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
     * Gets the amount of time (in milliseconds) it takes to send and receive data from the Server the Client is connected to.
     * @return Returns the amount of time (in milliseconds) it takes to send and receive data from the Server the Client is connected to. Returns -1 if not connected.
     */
    public int ping()
    {
        return ping;
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
