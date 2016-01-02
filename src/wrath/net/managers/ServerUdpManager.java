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
import java.util.HashMap;
import wrath.net.ConnectionState;
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
        state = ConnectionState.SOCKET_NOT_BOUND;
        this.recvThread = new Thread(() ->
        {
            final byte[] buf = new byte[Server.getServerConfig().getInt("UdpClientRecvBufferSize", 512)];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while(svr.isBound() && !recvFlag)
            {
                try
                {
                    svr.receive(packet);
                    String ident = packet.getAddress().toString() + ":" + packet.getPort();
                    if(!idenToClient.containsKey(ident))
                    {
                        ServerClient newClient = new ServerClient(server, packet.getAddress(), packet.getPort());
                        System.out.println("[NET_SERVER] Client connected from " + newClient.getClientIdentifier() + ".");
                        clients.add(newClient);
                        idenToClient.put(ident, newClient);
                        onClientConnect(newClient);
                    }
                    else
                    {
                        ServerClient c = idenToClient.get(ident);
                        onReceive(c, new Packet(packet.getData()));
                    }
                }
                catch(IOException ex)
                {
                    if(!recvFlag && isBound()) System.err.println("[NET_SERVER] Could not read data from UDP client! I/O Error!");
                }
            }
        });
        
        execThread.setDaemon(true);
    }

    @Override
    public synchronized  void bindSocket(String ip, int port)
    {
        if(isBound()) return;
        
        if(ip == null) ip = "*";
        this.ip = ip;
        this.port = port;
        
        recvFlag = false;
        state = ConnectionState.BINDING_PORT;
        try
        {
            if("*".equals(ip)) svr = new DatagramSocket(port);
            else svr = new DatagramSocket(port, InetAddress.getByName(ip));
            System.out.println("[NET_SERVER] ServerSocket bound to [" + ip + ":" + port + "].");
        }
        catch(SocketException ex)
        {
            System.err.println("[NET_SERVER] Could not bind port to [" + ip + ":" + port + "]! Socket Error/Port '" + port + "' already bound!");
            state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
        }
        catch(UnknownHostException e)
        {
            System.err.println("[NET_SERVER] Could not bind port to [" + ip + ":" + port + "]! Unkown binding IP '" + ip + "'!");
            state = ConnectionState.SOCKET_NOT_BOUND_ERROR;
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
        if(!clients.contains(client)) return;
        if(calledFirst)
        {
            System.out.println("[NET_SERVER] Disconnecting Client " + client.getClientIdentifier() + ".");
            send(client, Packet.TERMINATION_CALL);
        }
        else System.out.println("[NET_SERVER] Client " + client.getClientIdentifier() + " Disconnecting.");
        clients.remove(client);
        idenToClient.remove(client.getAddress().getHostAddress() + ":" + client.getPort());
        
        onClientDisconnect(client);
        
        System.out.println("[NET_SERVER] Client " + client.getClientIdentifier() + " Disconnected. ConnectionTime: " + ((double)(System.nanoTime() - client.getJoinTime())/1000000000) + "s");
    }
    
    @Override
    public boolean isBound()
    {
        if(svr == null) return false;
        return svr.isBound() && !svr.isClosed();
    }

    @Override
    public synchronized void send(ServerClient client, byte[] data)
    {
        if(clients.contains(client))
        {
            try
            {
                DatagramPacket pack = new DatagramPacket(data, data.length, client.getAddress(), client.getPort());
                svr.send(pack);
            }
            catch(IOException ex)
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
        
        clients.stream().forEach((c) ->
        {
            c.disconnectClient();
        });
        
        svr.close();
        
        state = ConnectionState.SOCKET_CLOSED;
        System.out.println("[NET_SERVER] ServerSocket Closed.");
        
        recvThread.stop();
        execThread.stop();
    }
}
