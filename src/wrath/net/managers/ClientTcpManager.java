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
            sock.setReceiveBufferSize(Client.getClientConfig().getInt("TcpProtocolRecvBufferSize", sock.getReceiveBufferSize()));
            sock.setSendBufferSize(Client.getClientConfig().getInt("TcpProtocolSendBufferSize", sock.getSendBufferSize()));
            sock.setReuseAddress(Client.getClientConfig().getBoolean("TcpReuseAddress", true));
            sock.setTrafficClass(Client.getClientConfig().getInt("TcpTrafficClass", sock.getTrafficClass()));
            sock.setOOBInline(Client.getClientConfig().getBoolean("TcpOobInline", sock.getOOBInline()));
        }
        catch(SocketException e)
        {
            System.err.println("Could not set TCP Socket properties! I/O Error!");
        }
        
        state = ConnectionState.DISCONNECTED_IDLE;
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("TcpRecvBufferSize", 512)];
            byte[] rbuf;
            while(isConnected() && !recvFlag)
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
                    if(isConnected() && !recvFlag) System.err.println("Could not read from input stream from [" + ip + ":" + port + "]!");
                    continue;
                }
                onReceive(client, new Packet(rbuf));
            }
        });
        
        execThread.setName("NetTcpClientExec");
        execThread.setDaemon(true);
        recvThread.setName("NetTcpClientRecv");
        recvThread.setDaemon(true);
    }
    
    @Override
    public synchronized void connect(String ip, int port)
    {
        if(isConnected()) return;
        
        this.ip = ip;
        this.port = port;
        
        state = ConnectionState.CONNECTING;
        System.out.println("Connecting to [" + ip + ":" + port + "]!");
        
        try
        {
            sock.connect(new InetSocketAddress(InetAddress.getByName(ip), port));
            recvFlag = false;
            recvThread.start();
            execThread.start();
            state = ConnectionState.CONNECTED;
            System.out.println("Connected to [" + ip + ":" + port + "]!");
        }
        catch(UnknownHostException e)
        {
            System.err.println("Could not resolve hostname/ip [" + ip + "]!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
        catch(IOException ex)
        {
            System.err.println("Could not connect to [" + ip + ":" + port + "]! I/O Error!");
            state = ConnectionState.DISCONNECTED_CONNECTION_FAILED;
        }
    }
    
    @Override
    public synchronized void disconnect(boolean calledFirst)
    {
        if(!isConnected()) return;
        recvFlag = true;
        if(!calledFirst) System.out.println("Received disconnect signal from host.");
        else send(new Packet(Packet.TERMINATION_CALL));
        System.out.println("Disconnecting from [" + ip + ":" + port + "]!");
        
        try
        {
            sock.close();
        }
        catch(IOException e)
        {
            state = ConnectionState.DISCONNECTED_CONNECTION_DROPPED;
            System.err.println("I/O Error occured while closing socket from [" + ip + ":" + port + "]!");
            return;
        }
        
        state = ConnectionState.DISCONNECTED_SESSION_CLOSED;
        System.out.println("Disconnected.");
        
        recvThread.stop();
        execThread.stop();
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
                System.err.println("Could not send data to [" + ip + ":" + port + "]! DataSize: " + data.length + "B");
            }
    }
}