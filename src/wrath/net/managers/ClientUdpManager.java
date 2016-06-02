/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import wrath.net.Client;
import wrath.net.Packet;

/**
 * Class to manage Client Connections using UDP.
 * @author Trent Spears
 */
public class ClientUdpManager extends ClientManager
{
    protected DatagramSocket sock = null;
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientUdpManager(Client client)
    {
        super(client);
    }
    
    @Override
    protected synchronized void createNewSocket(InetSocketAddress addr) throws IOException
    {
        // Define Object
        this.sock = new DatagramSocket();
        
        // Set Object Properties
        try
        {
            sock.setSoTimeout(Client.getClientConfig().getInt("Timeout", 500));
            sock.setReceiveBufferSize(Client.getClientConfig().getInt("UdpRecvBufferSize", sock.getReceiveBufferSize()));
            sock.setBroadcast(Client.getClientConfig().getBoolean("UdpSBroadcast", sock.getBroadcast()));
            sock.setSendBufferSize(Client.getClientConfig().getInt("UdpSendBufferSize", sock.getSendBufferSize()));
            sock.setReuseAddress(Client.getClientConfig().getBoolean("UdpReuseAddress", sock.getReuseAddress()));
            sock.setTrafficClass(Client.getClientConfig().getInt("UdpTrafficClass", sock.getTrafficClass()));
        }
        catch(SocketException ex)
        {
            System.err.println("] ERROR:  Could not set UDP Socket properties! I/O Error!");
        }
        
        // Define Receive Thread
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("UdpRecvArraySize", 512)];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            byte[] rbuf;
            while(!recvFlag && isConnected())
            {
                try
                {
                    sock.receive(packet);
                    rbuf = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, rbuf, 0, packet.getLength());
                    receive(client, new Packet(rbuf));
                }
                catch(IOException ex){}
            }
        });
        
        // Connect
        sock.connect(addr);
        sock.send(new DatagramPacket(new byte[]{0}, 1));
    }
    
    @Override
    protected synchronized void closeSocket()
    {
        sock.disconnect();
        sock.close();
    }
    
    /**
     * Gets the {@link java.net.DatagramSocket} used by this Client.
     * @return Returns the {@link java.net.DatagramSocket} used by this Client.
     */
    public DatagramSocket getRawSocket()
    {
        return sock;
    }
    
    @Override
    public boolean isConnected()
    {
        return sock != null && sock.isConnected() && !sock.isClosed();
    }
    
    @Override
    protected synchronized void pushData(byte[] data)
    {
        try
        {
            sock.send(new DatagramPacket(data, data.length));
        }
        catch(IOException ex)
        {
            System.err.println("] ERROR:  Could not send data to [" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "]! I/O Error!");
        }
    }
}
