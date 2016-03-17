/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import wrath.net.Client;

/**
 * Class to manage Client Connections using RUDP.
 * @author Trent Spears
 */
public class ClientRudpManager extends ClientUdpManager
{
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientRudpManager(Client client) 
    {
        super(client);
    }
    
}
