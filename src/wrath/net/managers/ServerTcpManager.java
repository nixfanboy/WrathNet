/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2016 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import wrath.net.Packet;
import wrath.net.Server;
import wrath.net.ServerClient;

/**
 * Class to manage Server Connections using TCP.
 * @author Trent Spears
 */
public class ServerTcpManager extends ServerManager
{
    private final HashMap<ServerClient, Socket> clientToSock = new HashMap<>();
    private ServerSocket svr;
    
    /**
     * Constructor.
     * @param server The {@link wrath.net.Server} being managed. 
     */
    public ServerTcpManager(Server server)
    {
        super(server);
    }

    @Override
    protected synchronized void closeSocket()
    {
        try
        {
            svr.close();
        }
        catch(IOException ex)
        {
            System.err.println("] Error while closing Server Socket! I/O Error!");
        }
    }
    
    @Override
    protected synchronized void createSocket(String ip, int port) throws IOException
    {
        // Define Object
        svr = new ServerSocket();
        
        // Set Object Properties
        try
        {
            svr.setReceiveBufferSize(Server.getServerConfig().getInt("TcpRecvBufferSize", svr.getReceiveBufferSize()));
            svr.setReuseAddress(Server.getServerConfig().getBoolean("TcpReuseAddress", true));
        }
        catch(SocketException e)
        {
            System.err.println("] Could not set TCP ServerSocket properties! I/O Error!");
        }
        
        // Define Receive Thread
        this.recvThread = new Thread(() ->
        {
            while(isBound() && !recvFlag)
            {
                try
                {
                    Socket s = svr.accept();
                    ServerClient c = new ServerClient(server, s.getInetAddress(), s.getPort());
                    Thread client = new Thread(() ->
                    {
                        System.out.println("] Client connected from " + c.getClientIdentifier() + ".");
                        clientToSock.put(c, s);
                        clients.add(c);
                        
                        onClientConnect(c);
                        
                        final byte[] buf = new byte[Server.getServerConfig().getInt("TcpClientRecvBufferSize", 1024)];
                        byte[] rbuf;
                        while(!recvFlag)
                        {
                            try
                            {
                                int len = s.getInputStream().read(buf);
                                if(len < 1 || s.isClosed()) break;
                                rbuf = new byte[len];
                                System.arraycopy(buf, 0, rbuf, 0, len);
                            }
                            catch(IOException e)
                            {
                                if(!c.isConnected()) break;
                                else if(!recvFlag && isBound()) System.err.println("] Could not read data from " + c.getClientIdentifier() + "! I/O Error!");
                                continue;
                            }
                            receive(c, new Packet(rbuf));
                        }
                        
                        if(clients.contains(c))
                            try
                            {
                                System.err.println("] Client " + c.getClientIdentifier() + " unexpectedly disconnected!");
                                s.close();
                            }
                            catch(IOException e){}
                    });
                    client.setDaemon(true);
                    client.start();
                }
                catch(IOException ex)
                {
                    if(!recvFlag && isBound()) System.err.println("] Could not connect Client, I/O Error!");
                }
            }
        });
        
        // Bind
        int backlog = Server.getServerConfig().getInt("TcpBacklog", 0);
        if(backlog == 0)
            if("*".equals(ip)) svr.bind(new InetSocketAddress(port));
            else svr.bind(new InetSocketAddress(InetAddress.getByName(ip), port));
        else
            if("*".equals(ip)) svr.bind(new InetSocketAddress(port), backlog);
            else svr.bind(new InetSocketAddress(InetAddress.getByName(ip), port), backlog);
    }

    /**
     * Gets the {@link java.net.ServerSocket} used by this Server.
     * @return Returns the {@link java.net.ServerSocket} used by this Server.
     */
    public ServerSocket getRawSocket()
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
        if(clientToSock.containsKey(client))
        {
            Socket s = clientToSock.get(client);
            try
            {
                s.getOutputStream().write(data);
                s.getOutputStream().flush();
            }
            catch(IOException e)
            {
                System.err.println("] Could not send data to " + client.getClientIdentifier() + "! DataSize: " + data.length + "B");
            }
        }
        else System.out.println("] WARNING: Attempted to send data to unknown client!");
    }
    
    @Override
    protected synchronized void removeClient(ServerClient client)
    {
        if(!clientToSock.containsKey(client)) return;
        Socket s = clientToSock.get(client);
        clientToSock.remove(client);
        try
        {
            if(s != null)
            {
                s.shutdownInput();
                s.close();
            }
        }
        catch(IOException ex)
        {
            System.err.println("] Could not close connection from " + client.getClientIdentifier() + "! I/O Error!");
        }
    }
}
