/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import wrath.net.Client;
import wrath.net.ConnectionState;
import wrath.net.Packet;

/**
 * Class to manage Client Connections using UDP.
 * @author Trent Spears
 */
public class ClientUdpManager extends ClientManager
{
    private DatagramSocket sock;
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientUdpManager(Client client)
    {
        super(client);
        try
        {
            this.sock = new DatagramSocket();
            sock.setSoTimeout(Client.getClientConfig().getInt("UdpTimeout", 500));
            sock.setReceiveBufferSize(Client.getClientConfig().getInt("UdpProtocolRecvBufferSize", sock.getReceiveBufferSize()));
            sock.setBroadcast(Client.getClientConfig().getBoolean("UdpSoBroadcast", sock.getBroadcast()));
            sock.setSendBufferSize(Client.getClientConfig().getInt("UdpProtocolSendBufferSize", sock.getSendBufferSize()));
            sock.setReuseAddress(Client.getClientConfig().getBoolean("UdpReuseAddress", sock.getReuseAddress()));
            sock.setTrafficClass(Client.getClientConfig().getInt("UdpTrafficClass", sock.getTrafficClass()));
        }
        catch(SocketException ex)
        {
            System.err.println("] Could not bind UDP Socket! Socket Error!");
        }
        
        state = ConnectionState.DISCONNECTED_IDLE;
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("UdpRecvBufferSize", 512)];
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
        
        execThread.setName("NetUdpClientExec");
        execThread.setDaemon(true);
        recvThread.setName("NetUdpClientRecv");
        recvThread.setDaemon(true);
    }
    
    @Override
    public synchronized void connect(String ip, int port)
    {
        if(sock.isConnected()) return;
        
        this.ip = ip;
        this.port = port;
        
        state = ConnectionState.BINDING_PORT;
        System.out.println("] Connecting to [" + ip + ":" + port + "]!");
        
        try
        {
            sock.connect(InetAddress.getByName(ip), port);
            recvFlag = false;
            recvThread.start();
            execThread.start();
            state = ConnectionState.CONNECTED;
            System.out.println("] Connected to [" + ip + ":" + port + "]!");
            send("init");
        }
        catch(UnknownHostException ex)
        {
            System.err.println("] Could not resolve hostname/ip [" + ip + "]!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
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
        
        recvThread.stop();
        execThread.stop();
    }
    
    @Override
    public boolean isConnected()
    {
        return sock.isConnected();
    }
    
    @Override
    public synchronized void send(byte[] data)
    {
        if(isConnected())
            try
            {
                DatagramPacket pack = new DatagramPacket(data, data.length);
                sock.send(pack);
            }
            catch(IOException ex)
            {
                System.err.println("] Could not send data to [" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "]! I/O Error!");
            }
    }
}
