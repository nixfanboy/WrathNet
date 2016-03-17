/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import wrath.net.Server;

/**
 * Class to manage Server Connections using RUDP.
 * @author Trent Spears
 */
public class ServerRudpManager extends ServerUdpManager
{
    /**
     * Constructor.
     * @param server The {@link wrath.net.Server} being managed. 
     */
    public ServerRudpManager(Server server) 
    {
        super(server);
    }
    
}
