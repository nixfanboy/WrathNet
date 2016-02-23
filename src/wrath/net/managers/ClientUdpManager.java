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
import wrath.net.ConnectionState;
import wrath.net.Packet;
import wrath.net.SessionFlag;
import wrath.util.Compression;

/**
 * Class to manage Client Connections using UDP.
 * @author Trent Spears
 */
public class ClientUdpManager extends ClientManager
{
    private DatagramSocket sock = null;
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientUdpManager(Client client)
    {
        super(client);
        state = ConnectionState.DISCONNECTED_IDLE;
    }
    
    @Override
    protected synchronized void createNewSocket(InetSocketAddress addr) throws IOException
    {
        try
        {
            this.sock = new DatagramSocket();
            sock.setSoTimeout(Client.getClientConfig().getInt("Timeout", 500));
            sock.setReceiveBufferSize(Client.getClientConfig().getInt("UdpRecvBufferSize", sock.getReceiveBufferSize()));
            sock.setBroadcast(Client.getClientConfig().getBoolean("UdpSBroadcast", sock.getBroadcast()));
            sock.setSendBufferSize(Client.getClientConfig().getInt("UdpSendBufferSize", sock.getSendBufferSize()));
            sock.setReuseAddress(Client.getClientConfig().getBoolean("UdpReuseAddress", sock.getReuseAddress()));
            sock.setTrafficClass(Client.getClientConfig().getInt("UdpTrafficClass", sock.getTrafficClass()));
        }
        catch(SocketException ex)
        {
            System.err.println("] Could not bind UDP Socket! Socket Error!");
        }
        
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("UdpRecvArraySize", 512)];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while(!recvFlag && isConnected())
            {
                try
                {
                    sock.receive(packet);
                    onReceive(client, new Packet(packet.getData()));
                }
                catch(IOException ex){}
            }
        });
        
        sock.connect(addr);
    }
    
    @Override
    public synchronized void disconnect(boolean calledFirst)
    {
        if(!isConnected()) return;
        recvFlag = true;
        if(!calledFirst) System.out.println("] Received disconnect signal from host.");
        else send(new Packet(Packet.TERMINATION_CALL));
        System.out.println("] Disconnecting from [" + ip + ":" + port + "]!");
        
        sock.disconnect();
        sock.close();
        
        state = ConnectionState.DISCONNECTED_SESSION_CLOSED;
        System.out.println("] Disconnected.");
    }
    
    @Override
    public boolean isConnected()
    {
        return sock != null && sock.isConnected();
    }
    
    @Override
    public synchronized void send(byte[] data)
    {
        if(isConnected())
            try
            {
                if(client.getSessionFlags().contains(SessionFlag.GZIP_COMPRESSION)) data = Compression.compressData(data);
                DatagramPacket pack = new DatagramPacket(data, data.length);
                sock.send(pack);
            }
            catch(IOException ex)
            {
                System.err.println("] Could not send data to [" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "]! I/O Error!");
            }
    }
}
