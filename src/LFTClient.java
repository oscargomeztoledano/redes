import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class LFTClient{
    private static String host;
    private static boolean SSL;
    private static final Logger errores=Logger.getLogger("errores");
    private static final Logger acciones=Logger.getLogger("acciones");
    private static int puerto;
    private static String clienteDir;
    private static Socket cliente;

    public static void main(String[] args) throws Exception{
        File file = new File("logErrores.log");    //creamos los archivos de registro
        File file2= new File("logAcciones.log");
        if (!file.exists()&&!file2.exists()) {
            try {
                file.createNewFile();
                file2.createNewFile();
            } catch (IOException e) {
                System.err.println("Error al crear el archivo de registro: " + e.getMessage());
            }
        }

        //asignamos al logger acciones el manejador que refiere al archivo logAcciones.log
        FileHandler handlerAcciones= new FileHandler("logAcciones.log",true);
        handlerAcciones.setFormatter(new SimpleFormatter());
        acciones.setLevel(Level.INFO);
        acciones.addHandler(handlerAcciones);

        //asignamos al logger errores el manejador que refiere al archivo logErrores.log
        FileHandler handlerErrores= new FileHandler("logErrores.log", true);
        handlerErrores.setFormatter(new SimpleFormatter());
        errores.setLevel(Level.WARNING);
        errores.addHandler(handlerErrores);

        //captura de argumentos

        try{

            if(args.length==4){
                if(args[0].equals("SSL")){
                    System.out.println("Modo ssl seleccionado.");
                    SSL=true;
                    puerto=Integer.parseInt(args[2]);
                    host=args[1];
                    clienteDir=args[3];

                }else{
                    errores.severe("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <host> <puerto> <Directorio_cliente>");
                    throw new IllegalArgumentException("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <puerto> <directorio_servidor> <clientes_max>");
                }
            }else if( args.length==3){
                System.out.println("Modo NOSSL seleccionado.");
                puerto=Integer.parseInt(args[1]);
                host=args[0];
                clienteDir=args[2];
            }

        }catch(NumberFormatException e){
            e.getMessage();
        }
        if (SSL) {
            LFTClient clienteSSL = new LFTClient(host, puerto);
            clienteSSL.start();
        } else {
            LFTClient cliente = new LFTClient();
            cliente.start();
        }
    }
    LFTClient(String host,int puerto) throws Exception{
        //modo ssl del servidor
    	String clientStore="C:\\Program Files\\Java\\jre1.8.0_321\\bin\\clientKey.jks";
    	String cacerts="C:\\Program Files\\Java\\jre1.8.0_321\\bin\\cacerts";

        //acedemos al almacen de claves serverkey
        KeyStore store= KeyStore.getInstance("JKS");
        store.load(new FileInputStream(clientStore), "clientpass".toCharArray());
        KeyManagerFactory Mfact=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        Mfact.init(store, "clientpass".toCharArray());
        KeyManager[] keyManagers= Mfact.getKeyManagers();

        //acedemos al almacen de claves trustedstore
        KeyStore trusted= KeyStore.getInstance("JKS");
        trusted.load(new FileInputStream(cacerts), "changeit".toCharArray());
        TrustManagerFactory tmFact=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFact.init(trusted);
        TrustManager[] trustManagers=tmFact.getTrustManagers();

        try{
            SSLContext socket=SSLContext.getInstance("SSL");
            //cliente identificado
            socket.init(keyManagers, trustManagers, null);
            SSLSocketFactory socketFactory=socket.getSocketFactory();   //creamos los sockets
            cliente=(SSLSocket) socketFactory.createSocket(host,puerto);

            ((SSLSocket)cliente).addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent ce){
                    X509Certificate c;
                    try{
                        c=(X509Certificate) ce.getPeerCertificates()[0];
                        String cname=c.getSubjectX500Principal().getName().substring(3, c.getSubjectX500Principal().getName().indexOf(","));
                        System.out.println("Nos conectamos al servidor con el certificado: "+ cname);
                    }catch(SSLPeerUnverifiedException e){
                        e.printStackTrace();
                    }
                }
            });
            ((SSLSocket)cliente).startHandshake();

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public LFTClient() throws UnknownHostException, IOException{
        cliente= new Socket(host, puerto);
    }
    public void start(){
        new Thread(){
            public void run(){
                try{
                    //salida para el servidor
                    PrintWriter salida=new PrintWriter(cliente.getOutputStream());

                    System.out.println("Acciones disponibles: \n"
                            + "GET + nombre del fichero\n"
                            + "LIST\n"
                            + "PUT + nombre del fichero\n");
                    Scanner entrada=new Scanner(System.in);
                    String eleccion= entrada.nextLine();
                    String tokens[]= eleccion.split(" ");

                    //Elección LIST
                    if (tokens[0].equals("LIST")){
                        acciones.info("Solicitud LIST.\n");
                        salida.println(eleccion);
                        salida.flush();
                        Scanner scaner= new Scanner(cliente.getInputStream());
                        acciones.info("Recibiendo listado de archivos");
                        System.out.println("Listado de archivos obtenido:");
                        while(scaner.hasNext()){
                            String line=scaner.nextLine();
                            String token[]=line.split(" ");
                            for (int j=0;j<token.length;j++)
                                System.out.println(token[j]+ " ");
                            System.out.println("\n");
                        }
                        cliente.close();
                    }

                    //Elección PUT
                    else if(tokens[0].equals("PUT")){
                        acciones.info("Solicitud PUT.\n");
                        salida.print(eleccion);
                        salida.flush();
                        String ruta= clienteDir+"\\"+ tokens[1];
                        File file=new File(ruta);
                        acciones.info("Enviamos el archivo: "+ruta+", al servidor");
                        if(file.isFile()&&file.exists()){
                            FileInputStream fichero=new FileInputStream(file);
                            byte[] buff=new byte[1024];
                            int bytesleidos;
                            while((bytesleidos=fichero.read(buff))!=-1)
                                cliente.getOutputStream().write(buff,0,bytesleidos);
                            fichero.close();
                            acciones.info("Archivo enviado");
                            System.out.println("Archivo enviado");
                        }else errores.severe("El archivo que se quiere enviar no existe");
                        cliente.close();
                    }

                    //Elección GET
                    else if(tokens[0].equals("GET")){
                        acciones.info("Solicitud GET.\n");
                        salida.println(eleccion);
                        salida.flush();

                        String fichero = clienteDir+"\\"+tokens[1];
                        InputStream i=cliente.getInputStream();
                        FileOutputStream o=new FileOutputStream(fichero);
                        acciones.info("Fichero: "+fichero+" pedido al servidor");


                        byte[] bff=new byte[1024];
                        int bytesleidos;
                        while((bytesleidos=i.read(bff))!=-1)
                            o.write(bff,0,bytesleidos);
                        acciones.info("Fichero guardado localmente.");
                        System.out.println("Fichero guardado localmente.");
                        o.close();
                        i.close();
                        cliente.close();
                    }
                    else errores.warning("El comando introducido no es invalido.");
                    entrada.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
    }
}