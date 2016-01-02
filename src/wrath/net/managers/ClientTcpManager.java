/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import wrath.net.Client;
import wrath.net.ConnectionState;
import wrath.net.Packet;

/**
 * Class to manage Client Connections using TCP.
 * @author Trent Spears
 */
public class ClientTcpManager extends ClientManager
{
    private final Socket sock = new Socket();
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientTcpManager(Client client)
    {
        super(client);
        
        try
        {
            sock.setSoTimeout(Client.getClientConfig().getInt("TcpTimeout", 500));
            sock.setKeepAlive(Client.getClientConfig().getBoolean("TcpKeepAlive", false));
            sock.setTcpNoDelay(Client.getClientConfig().getBoolean("TcpNoDelay", true));
        }
        catch(SocketException e)
        {
            System.err.println("[NET_CLIENT] Could not set TCP Socket properties! I/O Error!");
        }
        state = ConnectionState.DISCONNECTED_IDLE;
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("TcpRecvBufferSize", 512)];
            byte[] rbuf = new byte[0];
            while(client.isConnected() && !recvFlag)
            {
                try
                {
                    while(sock.getInputStream().available() < 1 && !sock.isClosed()) continue;
                    int len = sock.getInputStream().read(buf);
                    if(len < 1) break;
                    rbuf = new byte[len];
                    System.arraycopy(buf, 0, rbuf, 0, len);
                }
                catch(IOException e)
                {
                    System.err.println("[NET_CLIENT] Could not read from input stream from [" + ip + ":" + port + "]!");
                    continue;
                }
                Packet p = new Packet(rbuf);
                if(p.getDataAsObject() != null && p.getDataAsObject().equals(Packet.TERMINATION_CALL)) disconnect(false);
                else onReceive(client, p);
            }
        });
    }
    
    @Override
    public synchronized void connect(String ip, int port)
    {
        if(isConnected()) return;
        this.ip = ip;
        this.port = port;
        state = ConnectionState.CONNECTING;
        System.out.println("[NET_CLIENT] Connecting to [" + ip + ":" + port + "]!");
        try
        {
            sock.connect(new InetSocketAddress(InetAddress.getByName(ip), port));
            recvFlag = false;
            recvThread.start();
            execThread.setDaemon(true);
            execThread.start();
            System.out.println("[NET_CLIENT] Connected to [" + ip + ":" + port + "]!");
            state = ConnectionState.CONNECTED;
        }
        catch(UnknownHostException e)
        {
            System.err.println("[NET_CLIENT] Could not resolve hostname/ip [" + ip + "]!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
        catch(IOException ex)
        {
            System.err.println("[NET_CLIENT] Could not connect to [" + ip + ":" + port + "]! I/O Error!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
    }
    
    @Override
    public synchronized void disconnect(boolean calledFirst)
    {
        if(!client.isConnected()) return;
        recvFlag = true;
        if(!calledFirst) System.out.println("[NET_CLIENT] Received disconnect signal from host.");
        System.out.println("[NET_CLIENT] Disconnecting from [" + ip + ":" + port + "]!");
        //This termination call is specific to this net engine.
        if(calledFirst) send(new Packet(Packet.TERMINATION_CALL));
        try
        {
            sock.close();
            state = ConnectionState.DISCONNECTED_SESSION_CLOSED;
            System.out.println("[NET_CLIENT] Disconnected.");
        }
        catch(IOException e)
        {
            state = ConnectionState.DISCONNECTED_CONNECTION_DROPPED;
            System.err.println("[NET_CLIENT] I/O Error occured while closing socket from [" + ip + ":" + port + "]!");
        }
    }
    
    @Override
    public boolean isConnected()
    {
        return sock.isConnected() && !sock.isClosed();
    }
    
    @Override
    public synchronized void send(byte[] data)
    {
        if(client.isConnected()) 
            try 
            {
                sock.getOutputStream().write(data);
                sock.getOutputStream().flush();
            }
            catch(IOException ex) 
            {
                System.err.println("[NET_CLIENT] Could not send data to [" + ip + ":" + port + "]! DataSize: " + data.length + "B");
            }
    }
}