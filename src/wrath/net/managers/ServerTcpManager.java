/**
 * The MIT License (MIT)
 * Wrath Net Engine Copyright (c) 2015 Trent Spears
 */
package wrath.net.managers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import wrath.net.ConnectionState;
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
        try
        {
            this.svr = new ServerSocket();
        }
        catch(IOException ex)
        {
            System.err.println("[NET_SERVER] I/O Error occured while creating ServerSocket object. Cannot create server.");
        }
        state = ConnectionState.SOCKET_NOT_BOUND;
        this.recvThread = new Thread(() ->
        {
            while(server.isBound() && !recvFlag)
            {
                try
                {
                    Socket s = svr.accept();
                    ServerClient c = new ServerClient(server, s.getInetAddress(), s.getPort());
                    Thread client = new Thread(() ->
                    {
                        System.out.println("[NET_SERVER] Client connected from " + c.getClientIdentifier() + ".");
                        clientToSock.put(c, s);
                        clients.add(c);
                        
                        onClientConnect(c);
                        
                        final byte[] buf = new byte[Server.getServerConfig().getInt("TcpClientRecvBufferSize", 512)];
                        byte[] rbuf;
                        while(!recvFlag)
                        {
                            try
                            {
                                while(s.getInputStream().available() < 1 && !s.isClosed()) continue;
                                if(s.isClosed()) break;
                                int len = s.getInputStream().read(buf);
                                if(len < 1) break;
                                rbuf = new byte[len];
                                System.arraycopy(buf, 0, rbuf, 0, len);
                            }
                            catch(IOException e)
                            {
                                if(!c.isConnected()) break;
                                else if(!recvFlag && isBound()) System.err.println("[NET_SERVER] Could not read data from " + c.getClientIdentifier() + "! I/O Error!");
                                continue;
                            }
                            onReceive(c, new Packet(rbuf));
                        }
                        
                        if(clients.contains(c))
                            try
                            {
                                System.err.println("[NET_SERVER] Client " + c.getClientIdentifier() + " unexpectedly disconnected!");
                                s.close();
                            }
                            catch(IOException e){}
                    });
                    client.setDaemon(true);
                    client.start();
                }
                catch(IOException ex)
                {
                    if(!recvFlag && isBound()) System.err.println("[NET_SERVER] Could not connect Client, I/O Error!");
                }
            }
        });
        execThread.setDaemon(true);
    }

    @Override
    public synchronized void bindSocket(String ip, int port)
    {
        if(isBound()) return;
        
        if(ip == null) ip = "*";
        this.ip = ip;
        this.port = port;
        
        recvFlag = false;
        state = ConnectionState.BINDING_PORT;
        
        int backlog = Server.getServerConfig().getInt("TcpBacklog", 0);
        if(backlog == 0)
        {
            try
            {
                if("*".equals(ip)) svr.bind(new InetSocketAddress(port));
                else svr.bind(new InetSocketAddress(ip, port));
                
                System.out.println("[NET_SERVER] ServerSocket bound to [" + ip + ":" + port + "].");
            }
            catch(IOException ex)
            {
                System.err.println("[NET_SERVER] Could not bind port to [" + ip + ":" + port + "]! Socket Error/Port '" + port + "' already bound!");
                state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
                return;
            }
        }
        else
        {
            try
            {
                if("*".equals(ip)) svr.bind(new InetSocketAddress(port), backlog);
                else svr.bind(new InetSocketAddress(ip, port), backlog);
                System.out.println("[NET_SERVER] ServerSocket bound to [" + ip + ":" + port + "].");
            }
            catch(IOException ex)
            {
                System.err.println("[NET_SERVER] Could not bind port to [" + ip + ":" + port + "]! Socket Error/Port '" + port + "' already bound!");
                state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
                return;
            }
        }

        if(!isBound())
        {
            System.err.println("[NET_SERVER] Could not bind ServerSocket to [" + ip + ":" + port + "]! UNKNOWN Error!");
            state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
        }
        else state = ConnectionState.LISTENING;
        
        recvThread.start();
        execThread.start();
    }

    @Override
    public synchronized void disconnectClient(ServerClient client, boolean calledFirst)
    {
        if(!clientToSock.containsKey(client)) return;
        Socket s = clientToSock.get(client);
        if(calledFirst)
        {
            System.out.println("[NET_SERVER] Disconnecting Client " + client.getClientIdentifier() + ".");
            if(s != null) client.send(Packet.TERMINATION_CALL);
        }
        else System.out.println("[NET_SERVER] Client " + client.getClientIdentifier() + " Disconnecting.");
        clients.remove(client);
        clientToSock.remove(client);
        
        onClientDisconnect(client);
        
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
            System.err.println("[NET_SERVER] Could not close connection from " + client.getClientIdentifier() + "! I/O Error!");
        }
        System.out.println("[NET_SERVER] Client " + client.getClientIdentifier() + " Disconnected. ConnectionTime: " + ((double)(System.nanoTime() - client.getJoinTime())/1000000000) + "s");
    }
    
    @Override
    public boolean isBound()
    {
        return svr.isBound() && !svr.isClosed();
    }

    @Override
    public synchronized void send(ServerClient client, byte[] data)
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
                System.err.println("[NET_SERVER] Could not send data to " + client.getClientIdentifier() + "! DataSize: " + data.length + "B");
            }
        }
        else System.out.println("[NET_SERVER] WARNING: Attempted to send data to unknown client!");
    }
    
    @Override
    public synchronized void unbindSocket()
    {
        if(!isBound()) return;
        System.out.println("[NET_SERVER] Closing ServerSocket.");
        recvFlag = true;
        
        server.getClients().stream().forEach((c) ->
        {
            c.disconnectClient();
        });
        
        try
        {
            svr.close();
        }
        catch(IOException ex)
        {
            System.err.println("[NET_SERVER] Error while closing Server Socket! I/O Error!");
            state = ConnectionState.SOCKET_CLOSED;
        }
        
        state = ConnectionState.SOCKET_CLOSED;
        System.out.println("[NET_SERVER] ServerSocket Closed.");
        
        recvThread.stop();
        execThread.stop();
    }
}
