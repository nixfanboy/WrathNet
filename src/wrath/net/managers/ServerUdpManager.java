/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import wrath.net.Packet;
import wrath.net.Server;
import wrath.net.ServerClient;

/**
 * Class to manage Server Connections using UDP.
 * @author Trent Spears
 */
public class ServerUdpManager extends ServerManager
{
    private final HashMap<String, ServerClient> idenToClient = new HashMap<>();
    private DatagramSocket svr = null;
    
    /**
     * Constructor.
     * @param server The {@link wrath.net.Server} being managed. 
     */
    public ServerUdpManager(Server server)
    {
        super(server);
    }

    @Override
    protected synchronized void closeSocket()
    {
        svr.close();
    }
    
    @Override
    protected synchronized void createSocket(String ip, int port) throws IOException
    {
        // Define Object
        if("*".equals(ip)) svr = new DatagramSocket(new InetSocketAddress(port));
        else svr = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(ip), port));
        
        // Set Object Properties
        try
        {
            svr.setReceiveBufferSize(Server.getServerConfig().getInt("UdpRecvBufferSize", svr.getReceiveBufferSize()));
            svr.setBroadcast(Server.getServerConfig().getBoolean("UdpSBroadcast", svr.getBroadcast()));
            svr.setSendBufferSize(Server.getServerConfig().getInt("UdpSendBufferSize", svr.getSendBufferSize()));
            svr.setReuseAddress(Server.getServerConfig().getBoolean("UdpReuseAddress", svr.getReuseAddress()));
            svr.setTrafficClass(Server.getServerConfig().getInt("UdpTrafficClass", svr.getTrafficClass()));
        }
        catch(SocketException ex)
        {
            System.err.println("] Could not set UDP Socket properties! I/O Error!");
        }
        
        // Define Receive Thread
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Server.getServerConfig().getInt("UdpClientRecvBufferSize", 512)];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            byte[] rbuf;
            while(svr.isBound() && !recvFlag)
            {
                try
                {
                    svr.receive(packet);
                    String ident = packet.getAddress().toString() + ":" + packet.getPort();
                    if(!idenToClient.containsKey(ident))
                    {
                        ServerClient newClient = new ServerClient(server, packet.getAddress(), packet.getPort());
                        System.out.println("] Client connected from " + newClient.getClientIdentifier() + ".");
                        clients.add(newClient);
                        idenToClient.put(ident, newClient);
                        onClientConnect(newClient);
                    }
                    else
                    {
                        rbuf = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, rbuf, 0, packet.getLength());
                        receive(idenToClient.get(ident), new Packet(rbuf));
                    }
                }
                catch(IOException ex)
                {
                    if(!recvFlag && isBound()) System.err.println("] Could not read data from UDP client! I/O Error!");
                }
            }
        });
    }
    
    /**
     * Gets the {@link java.net.DatagramSocket} used by this Server.
     * @return Returns the {@link java.net.DatagramSocket} used by this Server.
     */
    public DatagramSocket getRawSocket()
    {
        return svr;
    }
    
    @Override
    public boolean isBound()
    {
        return svr != null && svr.isBound() && !svr.isClosed();
    }

    @Override
    protected synchronized void pushData(ServerClient client, byte[] data)
    {
        try
        {
            svr.send(new DatagramPacket(data, data.length, client.getAddress(), client.getPort()));
        } 
        catch(IOException ex)
        {
            System.err.println("] Could not send data to " + client.getClientIdentifier() + "! DataSize: " + data.length + "B");
        }
    }
    
    @Override
    protected synchronized void removeClient(ServerClient client)
    {
        idenToClient.remove(client.getAddress().getHostAddress() + ":" + client.getPort());
    }
}
