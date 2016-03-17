/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import wrath.net.Client;
import wrath.net.ConnectionState;
import wrath.net.Packet;

/**
 * Class to manage Client Connections using TCP.
 * @author Trent Spears
 */
public class ClientTcpManager extends ClientManager
{
    private Socket sock;
    
    /**
     * Constructor.
     * @param client The {@link wrath.net.Client} being managed.
     */
    public ClientTcpManager(Client client)
    {
        super(client);
    }
    
    @Override
    protected synchronized void createNewSocket(InetSocketAddress address) throws IOException
    {
        // Define Object
        sock = new Socket();
        
        // Set Object Properties
        try
        {
            sock.setSoTimeout(Client.getClientConfig().getInt("Timeout", 500));
            sock.setKeepAlive(Client.getClientConfig().getBoolean("TcpKeepAlive", false));
            sock.setTcpNoDelay(Client.getClientConfig().getBoolean("TcpNoDelay", true));
            sock.setReceiveBufferSize(Client.getClientConfig().getInt("TcpRecvBufferSize", sock.getReceiveBufferSize()));
            sock.setSendBufferSize(Client.getClientConfig().getInt("TcpSendBufferSize", sock.getSendBufferSize()));
            sock.setReuseAddress(Client.getClientConfig().getBoolean("TcpReuseAddress", true));
            sock.setTrafficClass(Client.getClientConfig().getInt("TcpTrafficClass", sock.getTrafficClass()));
            sock.setOOBInline(Client.getClientConfig().getBoolean("TcpOobInline", sock.getOOBInline()));
        }
        catch(SocketException e)
        {
            System.err.println("] Could not set TCP Socket properties! I/O Error!");
        }
        
        // Define Receive Thread
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Client.getClientConfig().getInt("TcpRecvArraySize", 512)];
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
                    if(isConnected() && !recvFlag) System.err.println("] Could not read from input stream from [" + ip + ":" + port + "]!");
                    continue;
                }
                onReceive(client, new Packet(rbuf));
            }
        });
        
        // Connect
        sock.connect(address, Client.getClientConfig().getInt("TcpConnectingTimeout", 1000));
    }
    
    @Override
    protected synchronized void closeSocket()
    {
        try
        {
            sock.close();
        }
        catch(IOException e)
        {
            state = ConnectionState.DISCONNECTED_CONNECTION_DROPPED;
            System.err.println("] I/O Error occured while closing socket from [" + ip + ":" + port + "]!");
            return;
        }
    }
    
    /**
     * Gets the {@link java.net.Socket} used by this Client.
     * @return Returns the {@link java.net.Socket} used by this Client.
     */
    public Socket getRawSocket()
    {
        return sock;
    }
    
    @Override
    public boolean isConnected()
    {
        return (sock != null && (sock.isConnected() && !sock.isClosed()));
    }
    
    @Override
    protected synchronized void pushData(byte[] data)
    {
        try 
        {
            sock.getOutputStream().write(data);
            sock.getOutputStream().flush();
        }
        catch (IOException ex) 
        {
            System.err.println("] Could not send data to [" + ip + ":" + port + "]! DataSize: " + data.length + "B");
        }
    }
}