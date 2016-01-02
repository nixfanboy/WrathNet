/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net;

/**
 * Interface to allow developer to handle data in their own way.
 * @author Trent Spears
 */
public interface ServerListener
{
    /**
     * Called when a {@link wrath.net.ServerClient} connects to the Server.
     * @param client The {@link wrath.net.ServerClient} that connected to the Server.
     */
    public void onClientConnect(ServerClient client);
    
    /**
     * Called when a {@link wrath.net.ServerClient} disconnects from the Server.
     * @param client The {@link wrath.net.ServerClient} that disconnected from the Server.
     */
    public void onClientDisconnect(ServerClient client);
    
    /**
     * Called when data is received from a Client to the Server.
     * @param client The {@link wrath.net.ServerClient} that the data originated from.
     * @param packet The {@link wrath.net.Packet} received from the Client.
     */
    public void onReceive(ServerClient client, Packet packet);
}
