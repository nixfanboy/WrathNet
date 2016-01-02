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
        }
        catch(SocketException ex)
        {
            System.err.println("[NET_CLIENT] Could not bind UDP Socket! Socket Error!");
        }
        state = ConnectionState.SOCKET_NOT_BOUND;
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("UdpRecvBufferSize", 512)];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while(!recvFlag && sock.isConnected())
            {
                try
                {
                    sock.receive(packet);
                    client.getClientListener().onReceive(client, new Packet(packet.getData()));
                }
                catch(IOException ex)
                {
                    System.err.println("[NET_CLIENT] Could not read data from [" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "]! I/O Error!");
                }
            }
        });
        
        recvThread.setDaemon(true);
    }
    
    @Override
    public synchronized void connect(String ip, int port)
    {
        if(sock.isConnected()) return;
        
        this.ip = ip;
        this.port = port;
        
        state = ConnectionState.BINDING_PORT;
        System.out.println("[NET_CLIENT] Connecting to [" + ip + ":" + port + "]!");
        try
        {
            sock.connect(InetAddress.getByName(ip), port);
            recvFlag = false;
            recvThread.start();
            execThread.setDaemon(true);
            execThread.start();
            state = ConnectionState.LISTENING;
            System.out.println("[NET_CLIENT] Connected to [" + ip + ":" + port + "]!");
            send("init");
        }
        catch(UnknownHostException ex)
        {
            System.err.println("[NET_CLIENT] Could not resolve hostname/ip [" + ip + "]!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
    }
    
    @Override
    public synchronized void disconnect(boolean calledFirst)
    {
        if(!isConnected()) return;
        if(calledFirst)
        {
            System.out.println("[NET_CLIENT] Disconnecting from [" + ip + ":" + port + "]!");
            client.send(Packet.TERMINATION_CALL);
        } 
        else System.out.println("[NET_CLIENT] Received disconnect signal from host!");
        recvFlag = true;
        sock.disconnect();
        sock.close();
        recvThread.stop();
        state = ConnectionState.SOCKET_CLOSED;
    }
    
    @Override
    public boolean isConnected()
    {
        return sock.isConnected();
    }
    
    @Override
    public synchronized void send(byte[] data)
    {
        if(!isConnected()) return;
        try
        {
            sock.send(new DatagramPacket(data, data.length));
        }
        catch(IOException ex)
        {
            System.err.println("[NET_CLIENT] Could not send data to [" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() + "]! I/O Error!");
        }
    }
}
