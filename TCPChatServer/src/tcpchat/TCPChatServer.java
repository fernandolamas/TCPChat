/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpchat;
import DiffieHellman.DH;
import static DiffieHellman.DH.miller_rabin;
import Entidades.Chat;
//import Encrypt.StringEncrypt;
//import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigInteger;
import javax.swing.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
//
////////////////////////////////////////////////////////////////////
// Action adapter for easy event-listener coding

public class TCPChatServer implements Runnable {
   public static String key1;
   public static Random randomGenerator = new Random();
   public static BigInteger generatorValue,primeValue = null,publicA,publicB,secretA = null,secretB,sharedKeyA,sharedKeyB;
   
    /**
     *
     */
   public final static int bitLength=105; // Con 105 bit obtengo una Key de 32 Bit para AES	
   public final static int certainty=20;// probabilistic prime generator 1-2^-certainty => practically 'almost sure'
   private static final SecureRandom rnd = new SecureRandom();
   public static String key;
   public static boolean HANDSHAKE = true;
   public static boolean ENCRYPTA = true;
   // Connect status constants
   public final static int NULL = 0;
   public final static int DISCONNECTED = 1;
   public final static int DISCONNECTING = 2;
   public final static int BEGIN_CONNECT = 3;
   public final static int CONNECTED = 4;
   // Other constants
   public final static String statusMessages[] = {
      " Error! Could not connect!", " Disconnected",
      " Disconnecting...", " Listenning...", " Connected"
   };
   public final static TCPChatServer tcpObj = new TCPChatServer();
   public final static String END_CHAT_SESSION =
   new Character((char)0).toString(); // Indicates the end of a session
   // Connection atate info
   public static String hostIP = "";
   public static int port = 8080;   
   public static int connectionStatus = DISCONNECTED;
   public static boolean isHost = true;
   public static String statusString = statusMessages[connectionStatus];
   public static StringBuffer toAppend = new StringBuffer("");
   public static StringBuffer toSend = new StringBuffer("");
   // Various GUI components and info
   public static JFrame mainFrame = null;
   public static JTextArea chatText = null;
   public static JTextField chatLine = null;
   public static JPanel statusBar = null;
   public static JLabel statusField = null;
   public static JTextField statusColor = null;
   public static JTextField ipField = null;
   public static JTextField portField = null;
   public static JTextField nickServerField = null;
   public static JRadioButton hostOption = null;
   public static JButton connectButton = null;
   public static JButton disconnectButton = null;
   public static JCheckBox encrypta = null;
   // TCP Components
   public static ServerSocket hostServer = null;
   public static Socket socket = null;
   public static BufferedReader in = null;
   public static PrintWriter out = null;
   /////////////////////////////////////////////////////////////////
   private static JPanel initOptionsPane() {
      JPanel pane = null;
      ActionAdapter buttonListener = null;
      // Create an options pane
      JPanel optionsPane = new JPanel(new GridLayout(5, 1));
      // IP address input
      pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      pane.add(new JLabel("Host IP:"));
      ipField = new JTextField(10); ipField.setText(hostIP);
      ipField.setEnabled(true);//(false);
      ipField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               ipField.selectAll();
               // Should be editable only when disconnected
               if (connectionStatus != DISCONNECTED) {
                  changeStatusNTS(NULL, true);
               }
               else {
                  hostIP = ipField.getText();
               }
            }
         });
      pane.add(ipField);
      optionsPane.add(pane);
      // Port input
      pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      pane.add(new JLabel("Port:"));
      portField = new JTextField(10); portField.setEditable(false);
      portField.setText((new Integer(port)).toString());
      portField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               // should be editable only when disconnected
               if (connectionStatus != DISCONNECTED) {
                  changeStatusNTS(NULL, true);
               }
               else {
                  int temp;
                  try {
                     temp = Integer.parseInt(portField.getText());
                     port = temp;
                  }
                  catch (NumberFormatException nfe) {
                     portField.setText((new Integer(port)).toString());
                     mainFrame.repaint();
                  }
               }
            }
         });
      pane.add(portField);
      optionsPane.add(pane);
      // Host/guest option
      buttonListener = new ActionAdapter() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if (connectionStatus != DISCONNECTED) {
                  changeStatusNTS(NULL, true);
               }
               else {
                  isHost = e.getActionCommand().equals("host");
                  // Cannot supply host IP if host option is chosen
                  if (isHost) {
                     ipField.setEnabled(false); //(false);
                     ipField.setText("localhost");
                     hostIP = "localhost";
                  }
                  else {
                     ipField.setEnabled(true);
                  }
               }
            }
         };
      // NICKNAME TEXT FIELD
      pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      pane.add(new JLabel("Nick:"));
      nickServerField = new JTextField(10); nickServerField.setEditable(true);
      nickServerField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               // should be editable only when disconnected
               if (connectionStatus != DISCONNECTED) {
                  changeStatusNTS(NULL, true);
               }
            }
      });
      pane.add(nickServerField);
      optionsPane.add(pane);
      
      ButtonGroup bg = new ButtonGroup();
      hostOption = new JRadioButton("Host", true);
      hostOption.setMnemonic(KeyEvent.VK_H);
      hostOption.setActionCommand("host");
      hostOption.addActionListener(buttonListener);
      bg.add(hostOption);
      
      pane = new JPanel(new GridLayout(1, 2));
      pane.add(hostOption);
      optionsPane.add(pane);
      // Connect/disconnect buttons
      JPanel buttonPane = new JPanel(new GridLayout(1, 2));
      buttonListener = new ActionAdapter() {
            @Override
            public void actionPerformed(ActionEvent e) {
               // Request a connection initiation
               if (e.getActionCommand().equals("connect")) {
                  changeStatusNTS(BEGIN_CONNECT, true);
               }
               // Disconnect
               else {
                  changeStatusNTS(DISCONNECTING, true);
               }
            }
         };
      
      encrypta = new JCheckBox("encrypta",true);
      encrypta.setText("ENCRYPTA");
      pane.add(encrypta);
      
      connectButton = new JButton("Connect");
      connectButton.setMnemonic(KeyEvent.VK_C);
      connectButton.setActionCommand("connect");
      connectButton.addActionListener(buttonListener);
      connectButton.setEnabled(true);
      disconnectButton = new JButton("Disconnect");
      disconnectButton.setMnemonic(KeyEvent.VK_D);
      disconnectButton.setActionCommand("disconnect");
      disconnectButton.addActionListener(buttonListener);
      disconnectButton.setEnabled(false);
      buttonPane.add(connectButton);
      buttonPane.add(disconnectButton);
      optionsPane.add(buttonPane);

      return optionsPane;
   }
   /////////////////////////////////////////////////////////////////
   // Initialize all the GUI components and display the frame
   private static void initGUI() {
      // Set up the status bar
      statusField = new JLabel();
      statusField.setText(statusMessages[DISCONNECTED]);
      statusColor = new JTextField(1);
      statusColor.setBackground(Color.red);
      statusColor.setEditable(false);
      statusBar = new JPanel(new BorderLayout());
      statusBar.add(statusColor, BorderLayout.WEST);
      statusBar.add(statusField, BorderLayout.CENTER);
      // Set up the options pane
      JPanel optionsPane = initOptionsPane();
      // Set up the chat pane
      JPanel chatPane = new JPanel(new BorderLayout());
      chatText = new JTextArea(10, 20);
      chatText.setLineWrap(true);
      chatText.setEditable(false);
      chatText.setForeground(Color.blue);
      JScrollPane chatTextPane = new JScrollPane(chatText,
         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      chatLine = new JTextField();
      chatLine.setEnabled(false);
      chatLine.addActionListener(new ActionAdapter() {
          private String dhkey;
            @Override
            public void actionPerformed(ActionEvent e) {
               String s_encrypted="";
               String s = chatLine.getText();
               if (!s.equals("")) {
                  chatLine.selectAll();
                  if (ENCRYPTA == true){
                    try {
                        System.out.println("Key: "+key);
                        s_encrypted = Encrypt.AES.encrypt(nickServerField.getText() + ": " + s + "\n", key);
                        appendToChatBox(nickServerField.getText() + ": " + s + "\n");
                        appendToChatBox(nickServerField.getText() + " ENC: " + s_encrypted + "\n");
                        sendString(s_encrypted);
                    } catch (Exception ex) {
                        Logger.getLogger(TCPChatServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                  } else {
                      appendToChatBox(nickServerField.getText() + ": " + s + "\n");
                      sendString(nickServerField.getText() + ": " + s + "\n");
                  }
                  chatLine.setText("");
               }
            }
      });
      chatPane.add(chatLine, BorderLayout.SOUTH);
      chatPane.add(chatTextPane, BorderLayout.CENTER);
      chatPane.setPreferredSize(new Dimension(200, 200));
      // Set up the main pane
      JPanel mainPane = new JPanel(new BorderLayout());
      mainPane.add(statusBar, BorderLayout.SOUTH);
      mainPane.add(optionsPane, BorderLayout.WEST);
      mainPane.add(chatPane, BorderLayout.CENTER);
      // Set up the main frame
      mainFrame = new JFrame("Simple TCP Chat - SERVER");
      mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainFrame.setContentPane(mainPane);
      mainFrame.setSize(mainFrame.getPreferredSize());
      mainFrame.setLocation(200, 200);
      mainFrame.pack();
      mainFrame.setVisible(true);
   }
   /////////////////////////////////////////////////////////////////
   // The thread-safe way to change the GUI components while
   // changing state
   private static void changeStatusTS(int newConnectStatus, boolean noError) {
      // Change state if valid state
      if (newConnectStatus != NULL) {
         connectionStatus = newConnectStatus;
      }
      // If there is no error, display the appropriate status message
      if (noError) {
         statusString = statusMessages[connectionStatus];
      }
      // Otherwise, display error message
      else {
         statusString = statusMessages[NULL];
      }
      // Call the run() routine (Runnable interface) on the
      // error-handling and GUI-update thread
      SwingUtilities.invokeLater(tcpObj);
   }
   /////////////////////////////////////////////////////////////////
   // The non-thread-safe way to change the GUI components while
   // changing state
   private static void changeStatusNTS(int newConnectStatus, boolean noError) {
      // Change state if valid state
      if (newConnectStatus != NULL) {
         connectionStatus = newConnectStatus;
      }
      // If there is no error, display the appropriate status message
      if (noError) {
         statusString = statusMessages[connectionStatus];
      }
      // Otherwise, display error message
      else {
         statusString = statusMessages[NULL];
      }
      // Call the run() routine (Runnable interface) on the
      // current thread
      tcpObj.run();
   }
   /////////////////////////////////////////////////////////////////
   // Thread-safe way to append to the chat box
   private static void appendToChatBox(String s) {
      synchronized (toAppend) {
         toAppend.append(s);
      }
   }
   /////////////////////////////////////////////////////////////////
   // Add text to send-buffer
   private static void sendString(String s) {
      synchronized (toSend) {
         toSend.append(s + "\n");
      }
   }
   /////////////////////////////////////////////////////////////////
   // Cleanup for disconnect
   private static void cleanUp() {
      try {
         if (hostServer != null) {
            hostServer.close();
            hostServer = null;
         }
      }
      catch (IOException e) { hostServer = null; }
      
      try {
         if (socket != null) {
            socket.close();
            socket = null;
         }
      }
      catch (IOException e) { socket = null; }

      try {
         if (in != null) {
            in.close();
            in = null;
         }
      }
      catch (IOException e) { in = null; }

      if (out != null) {
         out.close();
         out = null;
         int FIRSTCONNECTION = 0;
   }
   }

    public TCPChatServer() {
        this.key = "";
    }
   /////////////////////////////////////////////////////////////////
   // Checks the current state and sets the enables/disables
   // accordingly
   @Override
   public void run() {
      switch (connectionStatus) {
      case DISCONNECTED:
         connectButton.setEnabled(true);
         disconnectButton.setEnabled(false);
         ipField.setEnabled(true);
         portField.setEnabled(true);
         hostOption.setEnabled(true);
         nickServerField.setEnabled(true);
         chatLine.setText(""); chatLine.setEnabled(false);
         statusColor.setBackground(Color.red);
         break;

      case DISCONNECTING:
         connectButton.setEnabled(false);
         disconnectButton.setEnabled(false);
         ipField.setEnabled(false);
         portField.setEnabled(false);
         hostOption.setEnabled(false);
         nickServerField.setEnabled(false);
         chatLine.setEnabled(false);
         statusColor.setBackground(Color.orange);
         break;
      
      case CONNECTED:
         connectButton.setEnabled(false);
         disconnectButton.setEnabled(true);
         ipField.setEnabled(false);
         portField.setEnabled(false);
         hostOption.setEnabled(false);
         nickServerField.setEnabled(false);
         chatLine.setEnabled(true);
         statusColor.setBackground(Color.green);
         break;

      case BEGIN_CONNECT:
         connectButton.setEnabled(false);
         disconnectButton.setEnabled(false);
         ipField.setEnabled(false);
         portField.setEnabled(false);
         hostOption.setEnabled(false);
         nickServerField.setEnabled(false);
         chatLine.setEnabled(false);
         chatLine.grabFocus();
         statusColor.setBackground(Color.orange);
         break;
      }
      // Make sure that the button/text field states are consistent
      // with the internal states
      ipField.setText(hostIP);
      portField.setText((new Integer(port)).toString());
      hostOption.setSelected(isHost);
      statusField.setText(statusString);
      chatText.append(toAppend.toString());
      toAppend.setLength(0);

      mainFrame.repaint();
   }
   
   /////////////////////////////////////////////////////////////////
   // The main procedure
   public static void main(String args[]) {
      String s_encrypted="";
      String s_decrypted="";
      String s, toSendDB="", StringTEMP, StringTEMPIN, nickCliente="";
      initGUI();
      while (true) {
         try { // Poll every ~10 ms
            Thread.sleep(10);
         }
         catch (InterruptedException e) {}
         if (encrypta.isSelected()== false){
            ENCRYPTA = false;
         } else {
            ENCRYPTA = true;
         }
         switch (connectionStatus) {
         case BEGIN_CONNECT:
            try {
               // Try to set up a server if host
               if (isHost) {
                  hostServer = new ServerSocket(port);
                  socket = hostServer.accept();
               }
               // If guest, try to connect to the server
               else {
                  socket = new Socket(hostIP, port);
               }

               primeValue = findPrime();// BigInteger.valueOf((long)g);
               System.out.println("the prime is "+primeValue);
               generatorValue	= findPrimeRoot(primeValue);//BigInteger.valueOf((long)p);
               System.out.println("the generator of the prime is "+generatorValue);
               
               secretA = new BigInteger(bitLength-2,randomGenerator);
               System.out.println("The secret key for A is "+ secretA);
               publicA=generatorValue.modPow(secretA, primeValue);
               System.out.println("The Public key for A is " + publicA);

               //sharedKeyA = publicB.modPow(secretA,primeValue);
               //System.out.println("the shared key for A is "+sharedKeyA);
               
               in = new BufferedReader(new 
                  InputStreamReader(socket.getInputStream()));             
               s = in.readLine();
               nickCliente = s;
               // Envio el Nickname
               out = new PrintWriter(socket.getOutputStream(), true);              
               out.print(toSend.append(nickServerField.getText()+ ":" + publicA + "#" + primeValue + "#" + generatorValue).append("\n"));
               out.flush();
               toSend.setLength(0);

               changeStatusTS(CONNECTED, true);
            }
            // If error, clean up and output an error message
            catch (IOException e) {
               cleanUp();
               changeStatusTS(DISCONNECTED, false);
            }
            break;
             
         case CONNECTED:
                try {
                    Chat chat = new Chat();
                    // Send data
                    if (toSend.length() != 0) {
                        out.print(toSend);
                        out.flush();                  
                        String[] linesOUT = toSend.toString().split(System.getProperty("line.separator"));
                        StringTEMP = linesOUT[0]; // Saca el caracter de new line del mensaje.
                        String[] linesOUT1 = StringTEMP.split(":");
                        toSendDB = linesOUT1.toString();//[0];
                        System.out.println("send: "+toSendDB);
                        chat.setEmisor(nickServerField.getText());
                        chat.setReceptor(nickCliente);
                        chat.setMensaje(toSendDB.trim());
                        toSend.setLength(0);
                        changeStatusTS(NULL, true);
                    }                    
                    // Receive data
                    if (in.ready()) {
                        s = in.readLine();
                        if ((s != null) &&  (s.length() != 0)) {
                            // Check if it is the end of a trasmission
                            if (s.equals(END_CHAT_SESSION)) {
                                changeStatusTS(DISCONNECTING, true);
                            }
                            // Otherwise, receive what text
                            else {
                                String[] linesIN = s.split(System.getProperty("line.separator"));
                                System.out.println("recive: "+s);
                                if (!HANDSHAKE==true){
                                    if (ENCRYPTA == true){
                                        try {
                                            s_decrypted = Encrypt.AES.decrypt(s, key);
                                        } catch (Exception ex) {
                                            Logger.getLogger(TCPChatServer.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        System.out.println("recive dec: "+ s_decrypted);
                                        String[] x = s_decrypted.split(":");
                                        nickCliente = x[0];
                                        appendToChatBox(nickCliente +" ENC: " + s + "\n");
                                        appendToChatBox(s_decrypted + "\n");
                                    } else {
                                        System.out.println("recive dec: "+ s);
                                        String[] x = s.split(":");
                                        nickCliente = x[0];
                                        appendToChatBox(s + "\n");
                                    }
                                } else {
                                        System.out.println("recive: "+ s);
                                        String[] x = s.split(":");
                                        nickCliente = x[0];
                                        String publicSrvB = x[1];
                                        publicB = new BigInteger(publicSrvB.trim());
                                        sharedKeyA = publicB.modPow(secretA,primeValue);
                                        key=sharedKeyA.toString();
                                        System.out.println("the shared key is "+sharedKeyA);
                                        HANDSHAKE=false;
                                }
                                changeStatusTS(NULL, true);
                            }
                        }
                    }
                }
                catch (IOException e) {
                    cleanUp();
                    changeStatusTS(DISCONNECTED, false);
                }    
            break;

         case DISCONNECTING:
            // Tell other chatter to disconnect as well
            out.print(END_CHAT_SESSION); out.flush();
            // Clean up (close all streams/sockets)
            cleanUp();
            changeStatusTS(DISCONNECTED, true);
            break;

         default: break; // do nothing
         }
      }
   }
//####################################### CONJUNTO DE FUNCIONES PARA DIFFIE-HELLMAN #######################################\\
   public final static boolean isPrime(BigInteger r){
	return miller_rabin(r);
   }
   public final static java.util.List<BigInteger> primeFactors(BigInteger number) {
    BigInteger n = number;
	BigInteger i=BigInteger.valueOf(2);
	BigInteger limit=BigInteger.valueOf(10000);// speed hack! -> consequences ???
   	java.util.List<BigInteger> factors = new ArrayList<>();
   	while (!n.equals(BigInteger.ONE)){
		while (n.mod(i).equals(BigInteger.ZERO)){
        factors.add(i);
		n=n.divide(i);
		// System.out.println(i);
		// System.out.println(n);
		if(isPrime(n)){
			factors.add(n);// yes?
			return factors;
		}
     	}
		i=i.add(BigInteger.ONE);
		if(i.equals(limit))return factors;// hack! -> consequences ???
		// System.out.print(i+"    \r");
	}
		System.out.println(factors);
   return factors;
   }
   public final static String download(String address){
	String txt="";
   	URLConnection conn = null;
    InputStream in = null;
    try {
        URL url = new URL(address);
        conn = url.openConnection();
        conn.setReadTimeout(10000);//10 secs
        in = conn.getInputStream();
        byte[] buffer = new byte[1024];
        int numRead;
		String encoding = "UTF-8";
        while ((numRead = in.read(buffer)) != -1) {
				txt+=new String(buffer, 0, numRead, encoding);
        }
    } catch (Exception exception) {
        exception.printStackTrace();
    }
	return txt;
   }
   public final static boolean isPrimeRoot(BigInteger g, BigInteger p) {
    BigInteger totient = p.subtract(BigInteger.ONE); //p-1 for primes;// factor.phi(p);
    java.util.List<BigInteger> factors = primeFactors(totient);
    int i = 0;
    int j = factors.size();
    for(;i < j; i++)
    {
        BigInteger factor = factors.get(i);//elementAt
        BigInteger t = totient.divide( factor);
		if(g.modPow(t, p).equals(BigInteger.ONE))return false;
    }
    return true;
   }
   public final static void compareWolfram(BigInteger p){
	// String g= download("http://www.wolframalpha.com/input/?i=primitive+root+"+p);
	String url="http://api.wolframalpha.com/v2/query?appid=&input=primitive+root+"+p;
	System.out.println(url);
	String g= download(url);;
	String[] vals=g.split(".plaintext>");
	if(vals.length<3)	System.out.println(g);
	else System.out.println("wolframalpha generatorValue "+vals[3]);	
   }
   public final static BigInteger findPrimeRoot(BigInteger p){
	int start=2001;// first best probably precalculated by NSA?
	// preferably  3, 17 and 65537
	if(start==2)compareWolfram(p);

	for(int i=start;i<100000000;i++)
		if(isPrimeRoot(BigInteger.valueOf(i),p))
			return BigInteger.valueOf(i);
			// if(isPrimeRoot(i,p))return BigInteger.valueOf(i);
	return BigInteger.valueOf(0);
    }
    public final static BigInteger findPrime(){
	Random rnd=new Random();
	BigInteger p=BigInteger.ZERO;
	// while(!isPrime(p))
	p= new BigInteger(bitLength, certainty, rnd);// sufficiently NSA SAFE?!!
	return p;
		
    }
}
//####################################### CONJUNTO DE FUNCIONES PARA DIFFIE-HELLMAN #######################################\\
class ActionAdapter implements ActionListener {
   @Override
   public void actionPerformed(ActionEvent e) {}
}