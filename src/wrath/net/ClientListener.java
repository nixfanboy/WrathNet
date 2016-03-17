/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net;

/**
 * Interface to allow developer to handle data in their own way.
 * @author Trent Spears
 */
public interface ClientListener
{
    /**
     * Method called when data is received from a server.
     * @param client The {@link wrath.net.Client} that is receiving the data.
     * @param packet The {@link wrath.net.Packet} containing the data.
     */
    public void onReceive(Client client, Packet packet);
    
    /**
     * Method called when a Client connects to a server.
     * @param client The {@link wrath.net.Client} that connected to the server.
     */
    public void onConnect(Client client);
    
    /**
     * Method called when a Client disconnects from a server.
     * @param client The {@link wrath.net.Client} that disconnected from the server.
     */
    public void onDisconnect(Client client);
}
