/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package interfaces;

import java.util.Collection;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import static core.DTNHost.MaliciousNodes;
import static core.DTNHost.MsgInfo;
import static core.DTNHost.NodeInfo;
import static core.DTNHost.sortMsgInfo;
import core.NetworkInterface;
import core.Settings;
import static java.lang.Double.POSITIVE_INFINITY;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SimpleBroadcastInterface extends NetworkInterface {
	/**
	 * Reads the interface settings from the Settings file
	 *  
	 */
	public SimpleBroadcastInterface(Settings s)	{
		super(s);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SimpleBroadcastInterface(SimpleBroadcastInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new SimpleBroadcastInterface(this);
	}
        
        //FINDING AN ENTRY OF N2 IN N1 TABLE,NOT USED AS NOT NEEDED!!!
	public static boolean isFound(DTNHost n1,DTNHost n2)
        {
        DTNHost[] node = new DTNHost[1000];
        for(int i = 0; i <n1.NodeInfo.size(); i++)
            {
                ArrayList tmp =(ArrayList)NodeInfo.get(i);
                node[i]       = (DTNHost)tmp.get(0);
                if(node[i] == n2)return true;
            }
        
        return false;
        }
        
        //UPDATING A DTNHOST
        public static void NodeInfoUpdate(DTNHost n)
        {
         DTNHost[] src = new DTNHost[10000]; 
         DTNHost[] dest= new DTNHost[10000];
         int flag1 = 0,flag2 = 0;
         
         DTNHost[] node = new DTNHost[1000]; 
         int[]     FTT  = new int[1000];
         int[]     RTT  = new int[1000];
         double[]  Ratio= new double[1000];
         
         for(int i = 0; i <n.MsgInfo.size(); i++)
            {
                ArrayList tmp =(ArrayList)n.MsgInfo.get(i);
                src[i]        = (DTNHost)tmp.get(1);
                dest[i]       = (DTNHost)tmp.get(2);
            }
         for(int i = 0; i <n.NodeInfo.size(); i++)
            {
                ArrayList  tmp     =(ArrayList)n.NodeInfo.get(i);
                           
                           node[i] =(DTNHost)tmp.get(0); 
                           FTT[i]  =(int) tmp.get(1);
                           RTT[i]  =(int) tmp.get(2);
                           Ratio[i]=(int) tmp.get(3);
                
                if(src[i] == node[i])
                    {
                        
                        Ratio[i] = (double)((FTT[i]+1)/RTT[i]);
                        
                        tmp.set(1,FTT[i]+1);
                        tmp.set(3,Ratio[i]);    
                    }
                else 
                    {
                        ArrayList tmp2 = new ArrayList();
                        
                        tmp2.add(src[i]);
                        tmp2.add(1);
                        tmp2.add(0);
                        tmp2.add(POSITIVE_INFINITY);
                        
                        n.NodeInfo.add(tmp2);
                
                    }
                
                if(dest[i] == node[i])
                    {
                        Ratio[i] = (double)(FTT[i]/(RTT[i]+1));
                        
                        tmp.set(2,RTT[i]+1);
                        tmp.set(3,Ratio);    
                    }
                else 
                    {
                        ArrayList tmp2 = new ArrayList();
                        
                        tmp2.add(dest[i]);
                        tmp2.add(0);
                        tmp2.add(1);
                        tmp2.add(0);
                        
                        n.NodeInfo.add(tmp2);
                
                    }
            }
          
        }
        
        public static void getAllMsgs(DTNHost from,DTNHost to)
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
               sortMsgInfo(from); 
               
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
               sortMsgInfo(to); 
               
               }
             
             
             
             
             }
            
            
             }
        
        /**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		
                System.out.println("CONNECT!");
                
                if (isScanning()  
				&& anotherInterface.getHost().isActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
                         
			// new contact within range
			// connection speed is the lower one of the two speeds 
			
                        //System.out.println(anotherInterface.getHost().getAddress());
                        int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
                        
                        getAllMsgs(this.host,anotherInterface.getHost());
                        NodeInfoUpdate(this.host);
                       
                        /*
                        int n;
                        n = Array.getLength(DTNHost.MaliciousNodes);
                        
                        for (int i = 0; i < n; i++)
                        {
                              if (DTNHost.MaliciousNodes[i] == anotherInterface.getHost().getAddress())return;
                        }
                        */
                        
                        
                        connect(con,anotherInterface);
		}
	}

	/**
	 * Updates the state of current connections (ie tears down connections
	 * that are out of range).
	 */
	public void update() {
		// First break the old ones
		System.out.println("UPDATE!");
                optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);
                
			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";
                        
                        int f = 0;
                        for(int j = 0; j < MaliciousNodes.length; j++)
                                {
                                    if (anotherInterface.getHost().getAddress() == this.host.MaliciousNodes[j])
                                        f = 1;
                        
                                }
			
                        if (!isWithinRange(anotherInterface) || f == 1  ) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces) {
			
                        connect(i);
                      
		}
	
          }

	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	
        public void createConnection(NetworkInterface anotherInterface) {
                System.out.println("CREATE CONNECTION!");
		
                if (!isConnected(anotherInterface) && (this != anotherInterface)) {    			
			// connection speed is the lower one of the two speeds
                        //if (anotherInterface.getHost().getAddress() == 4)return;
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SimpleBroadcastInterface " + super.toString();
	}

}
