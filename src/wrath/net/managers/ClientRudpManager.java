/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import wrath.net.Client;
import wrath.net.Packet;

/**
 * Class to manage Client Connections using RUDP.
 * @author Trent Spears
 */
public class ClientRudpManager extends ClientUdpManager
{
    protected boolean conf = false;
    protected long pid = Long.MIN_VALUE;
    protected final ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientRudpManager(Client client) 
    {
        super(client);
    }

    @Override
    protected synchronized void createNewSocket(InetSocketAddress addr) throws IOException
    {
        super.createNewSocket(addr);
        send(Packet.RUDP_REQ);
    }
    
    @Override
    protected synchronized void pushData(byte[] data)
    {
        byte[] r = data;
        if(conf)
        {
            r = new byte[data.length + 1 + Long.BYTES];
            System.arraycopy(data, 0, r, 0, data.length);
            buf.putLong(pid++);
            System.arraycopy(buf.array(), 0, r, data.length, Long.BYTES);
            r[r.length - 1] = (char) 0;
            buf.clear();
        }
        
        super.send(r);
    }
}
