/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import static junit.runner.Version.id;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.sql.Types.NULL;
import java.util.Date;

import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.RoutingInfo;

/**
 * A DTN capable host.
 */

public class DTNHost implements Comparable<DTNHost> {
	private static int nextAddress = 0;
	private int address;

	private Coord location; 	// where is the host
	private Coord destination;	// where is it going

	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private List<NetworkInterface> net;
	private ModuleCommunicationBus comBus;
        
        public   ArrayList NodeInfo;
        public   ArrayList MaliciousNodes;
        public   ArrayList MsgInfo;
        public static double RatioThreshold;
        public static double SumThreshold;
        public Date date = new Date();
        public static List<Double> t1,t2;
        public static ArrayList tfr1,tfr2;
        public static int counter1,counter2;
        public int index;
        public String fileName ="G:\\One Simulator\\out" + ".txt";
        public FileInputStream fs;
        public BufferedReader br;
                /*
                br.write(Double.toString(maxRatio));
                br.newLine();
                br.write(Double.toString(maxSum));
                br.close();
                */
	static {
		DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
	}
	/**
         * 
         * 
	 * Creates a new DTNHost.
	 * @param msgLs Message listeners
	 * @param movLs Movement listeners
	 * @param groupId GroupID of this host
	 * @param interf List of NetworkInterfaces for the class
	 * @param comBus Module communication bus object
	 * @param mmProto Prototype of the movement model of this host
	 * @param mRouterProto Prototype of the message router of this host
	 */
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus, 
			MovementModel mmProto, MessageRouter mRouterProto) throws FileNotFoundException, IOException {
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = getNextAddress();
		this.name = groupId+address;
		this.net = new ArrayList<NetworkInterface>();

		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}	

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
                this.NodeInfo       = new ArrayList();
                this.MaliciousNodes = new ArrayList();
                this.MsgInfo        = new ArrayList();
                
                this.fs = new FileInputStream(fileName);
                this.br = new BufferedReader(new InputStreamReader(fs));
                this.RatioThreshold = Double.parseDouble(this.br.readLine());
                this.SumThreshold   = Double.parseDouble(this.br.readLine());
                this.counter1 = 0;
                this.counter2 = 0;
                this.t1 = new ArrayList();
                this.t2 = new ArrayList();
                this.tfr1 = new ArrayList();
                this.tfr2 = new ArrayList();
        }
	
	/**
	 * Returns a new network interface address and increments the address for
	 * subsequent calls.
	 * @return The next address.
	 */
	private static synchronized int getNextAddress() {
		return nextAddress++;	
	}

	/**
	 * Reset the host and its interfaces
	 */
	public static void reset() {
		nextAddress = 0;
	}

	/**
	 * Returns true if this node is active (false if not)
	 * @return true if this node is active (false if not)
	 */
	public boolean isActive() {
		return this.movement.isActive();
	}

	/**
	 * Set a router for this host
	 * @param router The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.address;
	}
	
	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}
	
    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
	}

	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a copy of the list of connections this host has with other hosts
	 * @return a copy of the list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> lc = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			lc.addAll(i.getConnections());
		}

		return lc;
	}

	/**
	 * Returns the current location of this host. 
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no
	 * path is in use at the moment.
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
	}


	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer 
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		double bSize = router.getBufferSize();
		double freeBuffer = router.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Returns the interface objects of the node
	 */
	public List<NetworkInterface> getInterfaces() {
		return net;
	}

	/**
	 * Find the network interface based on the index
	 */
	protected NetworkInterface getInterface(int interfaceNo) {
		NetworkInterface ni = null;
		try {
			ni = net.get(interfaceNo-1);
		} catch (IndexOutOfBoundsException ex) {
			System.out.println("No such interface: "+interfaceNo);
			System.exit(0);
		}
		return ni;
	}

	/**
	 * Find the network interface based on the interfacetype
	 */
	protected NetworkInterface getInterface(String interfacetype) {
		for (NetworkInterface ni : net) {
			if (ni.getInterfaceType().equals(interfacetype)) {
				return ni;
			}
		}
		return null;	
	}

	/**
	 * Force a connection event
	 */
	public void forceConnection(DTNHost anotherHost, String interfaceId, 
			boolean up) {
		NetworkInterface ni;
		NetworkInterface no;

		if (interfaceId != null) {
			ni = getInterface(interfaceId);
			no = anotherHost.getInterface(interfaceId);

			assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
			assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
		} else {
			ni = getInterface(1);
			no = anotherHost.getInterface(1);
			
			assert (ni.getInterfaceType().equals(no.getInterfaceType())) : 
				"Interface types do not match.  Please specify interface type explicitly";
		}
		
		if (up) {
			ni.createConnection(no);
		} else {
			ni.destroyConnection(no);
		}
	}

	/**
	 * for tests only --- do not use!!!
	 */
	public void connect(DTNHost h) {
		System.err.println(
				"WARNING: using deprecated DTNHost.connect(DTNHost)" +
		"\n Use DTNHost.forceConnection(DTNHost,null,true) instead");
		forceConnection(h,null,true);
	}

	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update(boolean simulateConnections) {
		if (!isActive()) {
			return;
		}
		
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
		this.router.update();
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is
	 * not time to move yet
	 * @param timeIncrement How long time the node moves
	 */
	public void move(double timeIncrement) {		
		double possibleMovement;
		double distance;
		double dx, dy;

		if (!isActive() || SimClock.getTime() < this.nextTimeToMove) {
			return; 
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				return; // no more waypoints left
			}
			distance = this.location.distance(this.destination);
		}

		// move towards the point for possibleMovement amount
		dx = (possibleMovement/distance) * (this.destination.getX() -
				this.location.getX());
		dy = (possibleMovement/distance) * (this.destination.getY() -
				this.location.getY());
		this.location.translate(dx, dy);
	}	

	/**
	 * Sets the next destination and speed to correspond the next waypoint
	 * on the path.
	 * @return True if there was a next waypoint to set, false if node still
	 * should wait
	 */
	private boolean setNextWaypoint() {
		if (path == null) {
			path = movement.getPath();
		}

		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		
             
             //System.out.println("SEND!");
            /*
            int n;
            n = Array.getLength(MaliciousNodes);
            for (int i = 0; i < n; i++)
            {
                if (MaliciousNodes[i] == to)return;
            }
            */
            this.router.sendMessage(id, to);
            //}
            }

	
	
        /*
        public void sortMsgInfo(DTNHost host)
        {
         ArrayList arr = new ArrayList();
         String[] str = new String[10000];
         DTNHost[] src  = new DTNHost[10000];
         DTNHost[] dest = new DTNHost[10000];
         int n = host.MsgInfo.size();
         
         for(int i = 0; i < n; i++)
         {
            arr = (ArrayList) host.MsgInfo.get(i);
            
            str[i] = (String) arr.get(0);
            src[i] = (DTNHost)arr.get(1);
            dest[i]= (DTNHost)arr.get(2);
         }
         int j;
         boolean flag = true;   
         String temp;
         DTNHost temp2;
         DTNHost temp3;
         while ( flag )
     {
            flag= false;    
            
            for( j=0;  j < n - 1;  j++ )
            {
                   if ( str[ j ].compareTo(str[j+1]) < 0 )  
                   {
                           temp = str[ j ];
                           temp2 = src[j];
                           temp3 = dest[j];
                           
                           str[ j ] = str  [ j+1 ];
                           src[j]   = src  [ j+1 ];
                           dest[j]  = dest [ j+1 ];
                           
                           str[ j+1 ] = temp;
                           src[ j+1 ] = temp2;
                           dest[ j+1 ] = temp3;
                           flag = true;         
                  } 
            } 
      }
                            host.MsgInfo = new ArrayList();
                            for(int i = 0; i < n; i++)
                            {
                                 ArrayList tmp = new ArrayList();
                                 
                                 tmp.add(str[i]);
                                 tmp.add(src[i]);
                                 tmp.add(dest[i]);
                                 host.MsgInfo.add(tmp);
        
                            }
        
        }
        */
        /**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
        
        
        //FINDING AN ENTRY OF N2 IN N1 TABLE
        public int NodePresentAlready(DTNHost n1,DTNHost n2)
        {
        DTNHost[] node = new DTNHost[1000];
        for(int i = 0; i <n1.NodeInfo.size(); i++)
            {
                ArrayList tmp =(ArrayList)n1.NodeInfo.get(i);
                node[i]       = (DTNHost)tmp.get(0);
                if(node[i] == n2)return i;
            }
        return -1;
        }
        
        public void NodeInfoUpdate(DTNHost n)
        {
         //n.MaliciousNodes[0] = 1;
         DTNHost[] src = new DTNHost[1000000]; 
         DTNHost[] dest= new DTNHost[1000000];   
         DTNHost[] node = new DTNHost[1000000]; 
         double[]     FTT  = new double[1000000];
         double[]     RTT  = new double[1000000];
         double[]  Ratio= new double[1000000];
         
         
         ArrayList tmpN = new ArrayList();
         //GETTING NODE INFO ENTRIES
         
         for(int i = 0; i <n.NodeInfo.size(); i++)
            {
                tmpN =(ArrayList)n.NodeInfo.get(i);
                node[i] =(DTNHost)tmpN.get(0); 
                FTT[i]  =(double) tmpN.get(1);
                RTT[i]  =(double) tmpN.get(2); 
                Ratio[i]=(double) tmpN.get(3);
            }
         
                int i = n.MsgInfo.size() - 1; 
                
                ArrayList  tmp     =(ArrayList)n.MsgInfo.get(i);
                           src[i]  = (DTNHost)tmp.get(2);
                          dest[i]  = (DTNHost)tmp.get(3);
                
                int tt = NodePresentAlready(n,src[i]);
         
                if(tt != -1 )
                    {
                        
                        //n.flag1[i] = true;
                        tmpN =(ArrayList)n.NodeInfo.get(tt);
                        DTNHost Entry = (DTNHost)tmpN.get(0);    
                        FTT[tt] = FTT[tt] + 1.0;
                        if( RTT[tt] == 0.0 ) Ratio[tt] = POSITIVE_INFINITY;//OK
                        else Ratio[tt] = (double)(FTT[tt]/RTT[tt]);
                        
                        //tmpN =(ArrayList)n.NodeInfo.get(tt);
                        tmpN.set(1,FTT[tt]);
                        tmpN.set(3,Ratio[tt]);
                        
                        if (Ratio [tt] > RatioThreshold && FTT[tt] + RTT[tt] > SumThreshold)
                        {
                         int e = Entry.getAddress();
                         int ii= MaliciousAlready(n,e);
                            if(ii == -1)
                            {
                                ArrayList tmp2 = new ArrayList();
                                tmp2.add(e);
                                tmp2.add(Long.valueOf(1));
                                n.MaliciousNodes.add(tmp2);
                            }
                            else 
                            {
                                ArrayList tmp2 = (ArrayList)n.MaliciousNodes.get(ii);
                                long h = (long)tmp2.get(1);
                                tmp2.set(1,Long.valueOf(h - 1));
                            }
                        }
                       
                    }
                else if(tt == -1)
                    {
                       // n.flag1[i] = true;
                        ArrayList tmp2 = new ArrayList();
                        
                        //FTT[n.NodeInfo.size()] = 1;
                        tmp2.add(src[i]);
                        tmp2.add(1.0);
                        tmp2.add(0.0);
                        tmp2.add(POSITIVE_INFINITY);
                        n.NodeInfo.add(tmp2);
                    }
                
                
                
                
                tt = NodePresentAlready(n,dest[i]);
                if(tt != -1)
                    {
                        
                       // n.flag2[i] = true;
                        tmpN =(ArrayList)n.NodeInfo.get(tt);
                        DTNHost Entry = (DTNHost)tmpN.get(0); 
                        RTT[tt] = RTT[tt] + 1.0;    
                        Ratio[tt] = (double)(FTT[tt]/RTT[tt]);
                        tmpN.set(2,RTT[tt]);
                        tmpN.set(3,Ratio[tt]);
                        
                        if (Ratio [tt] > RatioThreshold && FTT[tt] + RTT[tt] > SumThreshold)
                        {
                         int e = Entry.getAddress();
                         int ii = MaliciousAlready(n,e); 
                            if(ii == -1)
                            {
                                ArrayList tmp2 = new ArrayList();
                                tmp2.add(e);
                                tmp2.add(Long.valueOf(1));
                                n.MaliciousNodes.add(tmp2);
                            }
                            else 
                            {
                                ArrayList tmp2 = (ArrayList)n.MaliciousNodes.get(ii);
                                long h = (long)tmp2.get(1);
                                tmp2.set(1,Long.valueOf(h - 1));
                            }
                        }
                      
                    }
                else if(tt == -1)
                    {
                        //n.flag2[i] = true;
                        ArrayList tmp2 = new ArrayList();
                        
                        tmp2.add(dest[i]);
                        tmp2.add(0.0);
                        tmp2.add(1.0);
                        tmp2.add(0.0);
                        n.NodeInfo.add(tmp2);
                  
                    }
                //if (Ratio [i] != Threshold) n.MaliciousNodes[n.MaliciousNodes.length+1] = node[i].getAddress();
                
               
            
                        
                
         /*
         System.out.println("NODE INFO TABLE: "+n);
         
         for(int j = 0; j < n.NodeInfo.size(); j++)
                {
                double[]        R = new double[1000];
                DTNHost[]   node1 = new DTNHost[1000];
                ArrayList       t = (ArrayList)n.NodeInfo.get(j);
                              R[j]=(double)t.get(3);
                          node1[j]=(DTNHost)t.get(0);
                //if (R[j] != Threshold) n.MaliciousNodes[n.MaliciousNodes.length+1] = node1[j].getAddress();
                System.out.println(t);          
                }
          */
        }
        
        
        /*
        public  void getAllMsgs(DTNHost from,DTNHost to)
        {
         ArrayList fromMsg = from.MsgInfo;
         ArrayList toMsg   = to.MsgInfo;
         
         
         
         if(fromMsg.size() == 0 && toMsg.size() == 0) return;
         
         
         else
             {
               ArrayList Ti1 = (ArrayList) fromMsg.get(0);
               ArrayList Ti2 = (ArrayList) toMsg.get(0);
         
               String T1 = (String) Ti1.get(0);
               String T2 = (String) Ti2.get(0);  
               
               //update n1 node
               if (T2.compareTo(T1) > 0)
               {
                String[] Time  = new String[1000];   
                DTNHost[] src  = new DTNHost[1000];
                DTNHost[] dest = new DTNHost[1000];
                
                for(int i = 0; i < toMsg.size() ; i++)
                    {     
                         ArrayList tmp = (ArrayList)toMsg.get(i);
                         Time[i] = (String)tmp.get(0);
                         if(Time[i].compareTo(T1) > 0) fromMsg.add(tmp);
                         else break;
                    }
                
               from.MsgInfo = fromMsg;
               //sortMsgInfo(from); 
               
               }
               
               //update n2 node
               else if (T2.compareTo(T1) < 0)
               {
                String[] Time  = new String[1000];   
                DTNHost[] src  = new DTNHost[1000];
                DTNHost[] dest = new DTNHost[1000];
                
                for(int i = 0; i < fromMsg.size() ; i++)
                    {     
                         ArrayList tmp = (ArrayList)fromMsg.get(i);
                         Time[i] = (String)tmp.get(0);
                         if(Time[i].compareTo(T2) > 0) toMsg.add(tmp);
                         else break;
                    }
               to.MsgInfo = toMsg; 
               //sortMsgInfo(to); 
               
               }
             }
            }
        */
        public int MaliciousAlready(DTNHost n,int NodeEntry)
        {
        ArrayList tmp = new ArrayList();
        ArrayList tmp2= new ArrayList();
        int[] node  = new int[1000];
        long [] count= new  long[1000];
        for(int i = 0; i < n.MaliciousNodes.size();i++)
        {
            tmp        = (ArrayList) n.MaliciousNodes.get(i);
            node [i]   = (int)tmp.get(0);
            count [i]  = (long)tmp.get(1);
            
            if(node[i] == NodeEntry)
            {
                tmp2 =(ArrayList)n.MaliciousNodes.get(i);
                tmp2.set(1,Long.valueOf(count[i]+1)); 
                return i;
            }    
        }
        
        return -1;
        }
        
        
        public boolean isFound(DTNHost n,ArrayList Entry)
        {
        ArrayList tmp = new ArrayList();
        for(int i = 0; i < n.MsgInfo.size();i++)
        {
            tmp = (ArrayList)n.MsgInfo.get(i);
            if(tmp == Entry) return true;
        }
        
        return false;
        }
        public void getAllMsgs(DTNHost from,DTNHost to)
        {
        //from.MaliciousNodes[0] = 1;
        //to.MaliciousNodes[0] = 1;
        
        ArrayList tmp = new ArrayList();
            
        //ADDING IN FROM TABLE    
        for (int i = 0; i < to.MsgInfo.size(); i++)
            {
                
                tmp = (ArrayList) to.MsgInfo.get(i);
                if(!isFound(from,tmp))
                {
                    //System.out.println(tmp+" MSG ADDED IN:"+from);
                    from.MsgInfo.add(tmp);
                    NodeInfoUpdate(from);
                }
            }
        //ADDING IN TO TABLE    
        for (int i = 0; i < from.MsgInfo.size(); i++)
            {
                
                tmp = (ArrayList) from.MsgInfo.get(i);
                if(!isFound(to,tmp))
                {
                    //System.out.println(tmp+" MSG ADDED IN:"+to);
                    to.MsgInfo.add(tmp);
                    NodeInfoUpdate(to);
                }
        
            }
        }
        
        public void ShareMaliciousTables(DTNHost from,DTNHost to)
        {
        //from.MaliciousNodes[0] = 1;
        //to.MaliciousNodes[0] = 1;
        
        ArrayList prevTable = new ArrayList();
        prevTable = from.MaliciousNodes;
        int psize = prevTable.size();
        
        int[]     pnode= new int[1000];
        long[]   pcount= new long[1000];
        
        for(int i = 0; i < prevTable.size(); i++)
        {
            ArrayList tmp = new ArrayList();
            tmp = (ArrayList) prevTable.get(i);
            
            pnode[i]  =(int) tmp.get(0);
            pcount[i] =(long) tmp.get(1);
        
        }
        
        ArrayList tmp = new ArrayList();
        ArrayList tmp2= new ArrayList();
        int[]     node= new int[1000];
        long[]   count= new long[1000];
        
        //ADDING IN FROM TABLE    
        for (int i = 0; i < to.MaliciousNodes.size(); i++)
            {
                tmp = new ArrayList();
                tmp = (ArrayList) to.MaliciousNodes.get(i);
                
                node[i] = (int) tmp.get(0);
                count[i]= (long) tmp.get(1);
                
                if(MaliciousAlready(from,node[i]) == -1)
                {
                tmp2 = new ArrayList();
                tmp2.add(node[i]);
                tmp2.add(Long.valueOf(count[i]));
                from.MaliciousNodes.add(tmp2);
                    
                }
                
                else 
                {
                tmp2 = new ArrayList();
                int tt = MaliciousAlready(from,node[i]);
                
                tmp2 = (ArrayList) from.MaliciousNodes.get(tt);
                long get = (long)tmp2.get(1);
                long s = count[i] + get -2;
                tmp2.set(1,Long.valueOf(s));
                
                
                }
                
            }
        //ADDING IN TO TABLE    
        for (int i = 0; i < psize; i++)
            {
                
                tmp = new ArrayList();
                
                
                if(MaliciousAlready(to,pnode[i]) == -1)
                {
                tmp2 = new ArrayList();
                tmp2.add(pnode[i]);
                tmp2.add(Long.valueOf(pcount[i]));
                to.MaliciousNodes.add(tmp2);                  
                }
                
                else 
                {
                tmp2 = new ArrayList();
                int tt = MaliciousAlready(to,pnode[i]);
                
                tmp2 = (ArrayList) to.MaliciousNodes.get(tt);
                long get = (long)tmp2.get(1);
                long s = pcount[i] + get - 2 ;
                
                tmp2.set(1,Long.valueOf(s));
                
                
                }
                
        
            }
        }
        
        
        
        
        
        public int receiveMessage(Message m, DTNHost from) {
		
                
                int retVal = this.router.receiveMessage(m, from); 
                // MsgInfo.put(m, );
                if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}

		return retVal;	
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
        public void forceToBeMalicious(DTNHost node,String id)
        {
        if(node.getAddress()== 4) 
            {
            	System.out.println("---------------------------------------------NODE 4 DROPPED "+id);
            	ArrayList<Message> temp = 
				new ArrayList<Message>(this.getMessageCollection());
				for(int y=0;y<temp.size();y++)
				{
                                if(this.router.getMessage(id) == temp.get(y)) router.deleteMessage(id,true);;
                                }
	            	
	    }
        
        }
        
        public void messageTransferred(String id, DTNHost from) 
        {
            DTNHost to = this.router.getHost();
            //if (to.getAddress() == 4) forceToBeMalicious(to,id);
            
            //else 
            //{
            ArrayList tmp = new ArrayList();
            ArrayList tmp2 = new ArrayList();
            //String timeStamp = Long.toString(date.getTime());
            String timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
                
            
            tmp.add(id);
            tmp.add(timeStamp);
            tmp.add(from);
            tmp.add(to);
                
            if(!isFound(from,tmp))
            {
                from.MsgInfo.add(tmp);
                NodeInfoUpdate(from);
            }
            if(!isFound(to,tmp))  
            {
                to.MsgInfo.add(tmp);
                NodeInfoUpdate(to);
            }
                
            //sortMsgInfo(from);
            //sortMsgInfo(to);
            
            
            
            getAllMsgs(from,to);
            
            //NodeInfoUpdate(from);
            //NodeInfoUpdate(to);
            double[] test  = new double[1000000];
            double[] test1 = new double[1000000];
            System.out.println(id +" MSG TRANSMISSION: "+from+"->"+to);
            System.out.println("RATIO THRESHOLD: "+ RatioThreshold);
            System.out.println("SUM THRESHOLD: "+ SumThreshold);
            /*
            //MSG INFO
            System.out.println("MSG INFO TABLE OF NODE: "+from);
            for(int j = 0; j < from.MsgInfo.size(); j++)System.out.println(from.MsgInfo.get(j));
            System.out.println("MSG INFO TABLE OF NODE: "+to);
            for(int j = 0; j < to.MsgInfo.size(); j++)System.out.println(to.MsgInfo.get(j));
            */
            
            //NODE INFO
            if(from.NodeInfo.size() != 0)
            {
            System.out.println("NODE INFO TABLE OF NODE: "+from);
            for( int j = 0; j < from.NodeInfo.size(); j++) System.out.println(from.NodeInfo.get(j));
            }
            
            if(to.NodeInfo.size() != 0)
            {    
            System.out.println("NODE INFO TABLE OF NODE: "+to);
            for(int j = 0; j < to.NodeInfo.size(); j++) System.out.println(to.NodeInfo.get(j));
            }
            
            //MALICIOUS TABLE
            
            System.out.println("BEFORE SHARING: MALICIOUS NODE TABLE OF NODE: "+from);
            for(int j = 0; j < from.MaliciousNodes.size(); j++)
            {
                System.out.println(from.MaliciousNodes.get(j));
            }
            
            
            System.out.println("BEFORE SHARING: MALICIOUS NODE TABLE OF NODE: "+to);
            for(int j = 0; j < to.MaliciousNodes.size(); j++)
            {
                System.out.println(to.MaliciousNodes.get(j));
            }
            
            ShareMaliciousTables(from,to);  
            
          
            System.out.println("AFTER SHARING:MALICIOUS NODE TABLE OF NODE: "+from);
            for(int j = 0; j < from.MaliciousNodes.size(); j++)
            {
                System.out.println(from.MaliciousNodes.get(j));
            }
            
            
            System.out.println("AFTER SHARING: MALICIOUS NODE TABLE OF NODE: "+to);
            for(int j = 0; j < to.MaliciousNodes.size(); j++)
            {
                System.out.println(to.MaliciousNodes.get(j));
            }
        this.router.messageTransferred(id, from);
        
        
        
            
           // }
            
        }

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * @param m The message to create
	 */
	public void createNewMessage(Message m) {
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * @param otherHost The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}

}
