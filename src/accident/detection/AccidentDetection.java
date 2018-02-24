/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package accident.detection;
import javax.swing.*;    
import java.awt.event.*;
import com.pi4j.io.i2c.*;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.util.concurrent.TimeUnit;
import com.pi4j.gpio.extension.base.AdcGpioProvider;
import com.pi4j.gpio.extension.mcp.MCP3008GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP3008Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogInput;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.spi.SpiChannel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
/**
 *
 * @author pi
 */
public class AccidentDetection 
{
    /**
     * @param args the command line arguments
     */
    static int accidentflag;
    static int acclmed=5;
    static int acclhigh=10;
    static int gyrmed=45;
    static int gyrhigh=120;
    static int latency=100;
    static double gravity=9.81;
    static double fuzzyacc,fuzzygyr, fuzzyfsr;
    static int readword(int high, int low)//mpu6050 stores 2's compelment of values in register so we use this function and also because mpu6050 keeps high and low registers
    {
        int temp=(high<<8)+low;
        if(temp>=0x8000)
        {
        return(-((0xffff-temp)+1));
        }
        else
        {
        return(temp);
        }
    }
    public static void main(String[] args) throws Exception
    {
        double resistor=0;
        int power_mgmt_1 = 0x6b;
        int power_mgmt_2 = 0x6c;
        int accel_config_1=28;
        int xh,xl,yh,yl,zh,zl,xaddr=0x3b,yaddr=0x3d,zaddr=0x3f;
        int gxh,gxl,gyh,gyl,gzh,gzl,gxaddr=0x43,gyaddr=0x45,gzaddr=0x47;
        int ax,ay,az,gx,gy,gz,socket=6000;
        double A,G,Aadj;
        double accx,accy,accz,gyrx,gyry,gyrz;
        String FSR,ipaddr;
        DecimalFormat df=new DecimalFormat("00.00");
        // TODO code application logic here
        
        JMenuBar mb=new JMenuBar();  
        JMenu option=new JMenu("Options");
        JMenuItem o1,o2,o3,o4,o5;
        o1=new JMenuItem("Hide");  
        o2=new JMenuItem("Latency");  
        o3=new JMenuItem("Gravity");  
        o4=new JMenuItem("Change colour");  
        o5=new JMenuItem("Exit"); 
        option.add(o1);
        option.add(o2);
        option.add(o3);
        option.add(o4);
        option.add(o5);
        mb.add(option);
        JFrame f=new JFrame("Accident Detection using accelerometer");//creating instance of JFrame  
        f.add(mb);
        f.setJMenuBar(mb);  
        mb.setVisible(true);
        
        Process p = Runtime.getRuntime().exec("sudo arp-scan --retry=8 --ignoredups -I wlan0 --localnet");
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = "",temp="";
        while ((line = reader.readLine()) != null) 
        {
            if(line.contains(ClientThread.macaddr))
            {
                temp=line;
            }
        }
        String tmp=" ";
        String array[]=temp.split(ClientThread.macaddr);
        ipaddr=array[0].trim();
        Thread th=new Thread(new MainThread(ipaddr,socket));
        th.start();
        o1.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    f.setVisible(false);
                }
            });
        o2.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    if(latency==0)
                    {
                        latency=100;
                    }
                    else
                    {
                        if(latency==100)
                        {
                            latency=0;
                        }
                    }
                    JOptionPane.showMessageDialog(f,"Value of latency changed to: "+latency);
                }
            });
        o3.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    if(gravity==9.81)
                    {
                        gravity=9.8;
                    }
                    else
                    {
                        if(gravity==9.8)
                        {
                            gravity=9.81;
                        }
                    }
                    JOptionPane.showMessageDialog(f,"Value of g changed to: "+gravity);
                }
            });
        o4.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    f.getContentPane().setBackground(Color.yellow);
                }
            });
        o5.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    System.exit(0);
                }
            });
        JLabel l=new JLabel();
        l.setText("Hello1");
        JLabel l1=new JLabel();
        l1.setText("Hello2");
        JLabel l2=new JLabel();
        l2.setText("Hello3");
        JLabel l3=new JLabel();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(screenSize.width/2, screenSize.height/2);//400 width and 500 height  
        l.setBounds(0, 0, screenSize.width/2, screenSize.height/20);
        l1.setBounds(0, screenSize.height/20,screenSize.width/2, screenSize.height/20);
        l2.setBounds(0, screenSize.height/10,screenSize.width/2, screenSize.height/20);
        l3.setBounds(0,3*screenSize.height/20,screenSize.width/2, screenSize.height/20);
        f.setLayout(null);//using no layout managers  
        f.setVisible(true);//making the frame visible 
        l.setText("123 123");
        f.add(l);
        /*f.add(l1);*/
        f.add(l2);
        f.add(l3);
        l.setText("123 456");
        l1.setVisible(true);
        l1.setText("Hey there");
        f.add(l1);
        l3.setText("MAC address of smartphone: "+ClientThread.macaddr+"   "+"IP address of smartphone: "+ipaddr+"   "+"At socket number: "+socket);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // Create custom MCP3008 analog gpio provider
        // we must specify which chip select (CS) that that ADC chip is physically connected to.
        final AdcGpioProvider provider = new MCP3008GpioProvider(SpiChannel.CS0);

        // Provision gpio analog input pins for all channels of the MCP3008.
        // (you don't have to define them all if you only use a subset in your project)
        final GpioPinAnalogInput inputs[] = {
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH0, "CH0")/*,
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH1, "CH1"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH2, "CH2"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH3, "CH3"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH4, "CH4"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH5, "CH5"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH6, "CH6"),
                gpio.provisionAnalogInputPin(provider, MCP3008Pin.CH7, "CH7")*/
        };
        
        try
        {
            I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
            I2CDevice mpu6050 = bus.getDevice(0x68);
            byte p1=0x03;//1,2,3=pll with X,Y,Z-axis of Gyro for greater accuracy; 0=internal 8mhz oscillator
            mpu6050.write(power_mgmt_1,p1);
            mpu6050.write(accel_config_1,(byte)0);//00011000 ensures that +/-16g scale is used, 0 in decimal ensures +/-2g scale is used
            //mpu6050.write(power_mgmt_2); 
            for(;;)
            {
                xh=mpu6050.read(xaddr);//reading all sensor values from registers in xh,xl,yh,yl,zh,zl from accelerometer and gxh,gxl,gyh,gyl,gzh,gzl from gyroscope
                xl=mpu6050.read(xaddr+1);
                yh=mpu6050.read(yaddr);
                yl=mpu6050.read(yaddr+1);
                zh=mpu6050.read(zaddr);
                zl=mpu6050.read(zaddr+1); 
                gxh=mpu6050.read(gxaddr);
                gxl=mpu6050.read(gxaddr+1);
                gyh=mpu6050.read(gyaddr);
                gyl=mpu6050.read(gyaddr+1);
                gzh=mpu6050.read(gzaddr);
                gzl=mpu6050.read(gzaddr+1);
                ax=readword(xh,xl);
                ay=readword(yh,yl);
                az=readword(zh,zl);
                gx=readword(gxh,gxl);
                gy=readword(gyh,gyl);
                gz=readword(gzh,gzl);
                accx=(ax*gravity)/16384;//16384 is the value the accelerometer config gives for 2g in register as per mpu6050 documentation
                accy=(ay*gravity)/16384;
                accz=(az*gravity)/16384;
                /*accx=ax/1670.0;
                accy=ay/1670.0;
                accz=az/1670.0;*/
                gyrx=gx/131.0;
                gyry=gy/131.0;
                gyrz=gz/131.0; 
                A=Math.sqrt(accx*accx+accy*accy+accz*accz);
                G=Math.sqrt(gyry*gyry+gyrx*gyrx+gyrz*gyrz);
                Aadj=Math.abs(A-gravity);
                FSR="";
                for(GpioPinAnalogInput input : inputs)
                {
                    resistor=input.getValue();
                    FSR=FSR+("["+input.getName() + "= " + input.getValue()+ "]  ");
                }
                if(Aadj>0 && Aadj<acclmed)
                {
                    fuzzyacc=0;
                }
                else if(Aadj>=acclmed && Aadj<acclhigh)
                {
                    fuzzyacc=(Math.E-Math.log1p(acclhigh-Aadj*1.0))/Math.E;
                }
                else if(Aadj>=acclhigh)
                {
                    fuzzyacc=1;
                }
                if(G>0 && G<gyrmed)
                {
                    fuzzygyr=0;
                }
                else if(G>=gyrmed && G<gyrhigh)
                {
                    fuzzygyr=(Math.E-Math.log1p(gyrhigh-G*1.0))/Math.E;
                }
                else if(G>=gyrhigh)
                {
                    fuzzygyr=1;
                }
                if(fuzzyacc==1)
                {
                    accidentflag=1;
                }
                else if(fuzzyacc<=1 && fuzzyacc>0)
                {
                    if(fuzzygyr>0 && fuzzygyr<1)
                    {
                        if((fuzzyacc*0.85+fuzzygyr*0.10+(resistor*0.05/900))>0.5)
                        {
                           accidentflag=1; 
                        }
                    }
                    
                }
                if(ClientThread.running==true)
                {
                l2.setText("Connection status: OK");
                }
                else
                {
                l2.setText("Connection status: Not Connected");
                }
                if(accidentflag==1)
                {
                    accidentflag=0;
                    ClientThread.message="Accident";
                    JOptionPane.showMessageDialog(f,"Accident Occured Parameters: [ A: "+df.format(A)+"  Adjusted A:"+df.format(Aadj)+"  G:"+df.format(G)+" "+FSR+"]");
                } 
                l.setText("X: "+df.format(accx)+"  Y: "+df.format(accy)+"  Z: "+df.format(accz)+"  A: "+df.format(A)+"  Adjusted A:"+df.format(Aadj));//16384/g=1670 for 2g 16384
                l1.setText("X: "+df.format(gyrx)+"  Y: "+df.format(gyry)+"  Z: "+df.format(gyrz)+"  G:"+df.format(G)+" "+FSR);
                TimeUnit.MILLISECONDS.sleep(latency);
                if(ClientThread.running==false)
                {
                    {
                    ClientThread.running=true;
                    l2.setText("Connection status: Trying to connect....");
                    System.out.println("Trying to connect....");
                    th=new Thread(new MainThread(ipaddr,socket));
                    th.start();
                    }
                }
            }
        }
        catch(Exception e)
        {        
            JOptionPane.showMessageDialog(f,e.toString());
        }    
    }    
}
class ClientThread
{
    static boolean running=true;
    static String message="Alright";
    static String macaddr="7c:46:85:80:77:08";
}
class MainThread implements Runnable
{
    String ipaddr;
    int port;
    public MainThread(String a,int b)
    {
        this.ipaddr=a;
        this.port=b;
    }
    public void run()
    {
        Socket sock=null;
            try 
            {
                sock = new Socket(this.ipaddr,this.port);
                Thread thread = new Thread(new SendThread(sock));
                thread.start();
                Thread thread2 =new Thread(new RecieveThread(sock));
                thread2.start();
            } 
            catch (Exception e) 
            {
                System.out.println("ERROR!!______________");
                System.out.println(e.getMessage());
                ClientThread.running=false;
                
                //System.exit(0);
            }
        
    }
}           
            /*try
            {
            sock.close();
            }
            catch(Exception xp){System.exit(0);}*/
class RecieveThread implements Runnable
{
    Socket sock=null;
    BufferedReader recieve=null;
	
    public RecieveThread(Socket sock) //constructor to initialise the values
    {
	this.sock = sock;
    }
    public void run() 
    {
        try
        {
            recieve = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));//get inputstream
            String msgRecieved = "Hello";
		while(ClientThread.running==true)
		{
                    msgRecieved = recieve.readLine();
                    if(msgRecieved.equals(null))
                    {
                        break;
                    }
                    System.out.println("Server: " + msgRecieved);
                    System.out.println("");//add a space of one line
		}
                if(ClientThread.running==false)
                {
                    System.out.println("Exit from Receive Thread \n");
                }
        }
        catch(NullPointerException ex)
        {
            System.out.println("ERROR!!______________ receive thread");
            System.out.println(ex.getMessage());
            ClientThread.running=false;
            
        }
            catch(Exception e)
            {
            System.out.println("ERROR!!______________ receive thread");
            System.out.println(e.getMessage());
            ClientThread.running=false;
            
            //System.exit(0);
            }
	}//end run
}//end class recievethread
class SendThread implements Runnable
{
	Socket sock=null;
	PrintWriter print=null;
	BufferedReader brinput=null;
	
	public SendThread(Socket sock)
	{
		this.sock = sock;
	}//end constructor
	public void run()
        {
            try
            {
		if(sock.isConnected())
		{
                    System.out.println("Client connected to "+sock.getInetAddress() + " on port "+sock.getPort());
                    this.print = new PrintWriter(sock.getOutputStream(), true);	
                    while(ClientThread.running==true)
                    {
                        if(ClientThread.message.equals("Accident"))
                        {
			this.print.println(ClientThread.message);
			this.print.flush();
                        }
                    }//end while
		//sock.close();
                    if(ClientThread.running==false)
                    {
                        System.out.println("Exit from Send Thread \n");
                    }
                }
            }
            catch(Exception e)
            {                
                System.out.println("ERROR!!______________ send thread");
                System.out.println(e.getMessage());
                ClientThread.running=false;
                
                //System.exit(0);
            }
	}//end run method
}//end class

