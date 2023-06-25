import javax.net.ssl.SSLServerSocket;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Scanner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


public class LFTServer {
    private static final Logger errores=Logger.getLogger("errores");
    private static final Logger acciones=Logger.getLogger("acciones");
    private static int puerto;
    private static int clientesMax;
    private static String serverDir;
    private static boolean SSL=false;
    private static int nCLientes=0;
    private SSLServerSocket socketSSL;
    


    public static void main(String[] args) throws UnrecoverableKeyException,CertificateException,NoSuchAlgorithmException,IOException,KeyStoreException{
        
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
        

        try{
            if(args.length==4){     //seleccion de modo ssl
                if(args[0].equals("SSL")){  //parametros del modo ssl
                    System.out.println("Ha seleccionado el modo SSL");
                    SSL=true;
                    puerto=Integer.parseInt(args[1]);
                    clientesMax=Integer.parseInt(args[3]);
                    serverDir=args[2];

                }else{
                    errores.severe("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <puerto> <directorio_servidor> <clientes_max>");
                    throw new IllegalArgumentException("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <puerto> <directorio_servidor> <clientes_max>");
                }
            }else if(args.length==3){       //seleccion  de modo NOSSL
                System.out.println("Ha seleccionado el modo NOSSL");
                puerto=Integer.parseInt(args[0]);       //parametros del modo NOSSL
                clientesMax=Integer.parseInt(args[2]);
                serverDir=args[1];
            }else{
                errores.severe("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <puerto> <directorio_servidor> <clientes_max>");
                throw new IllegalArgumentException("ERROR FATAL, por favor introduce los argumentos en el siguiente orden: <modo> <puerto> <directorio_servidor> <clientes_max>");
            }
        }catch(NumberFormatException e){
            e.getMessage();
        }

        LFTServer server= new LFTServer();  //instanciamos esta misma clase
        if(SSL)server.modoSSL(puerto);      //llamamos dependiendo del modo que se haya seleccionado
        else server.modoNormal(puerto);
        

    }
    public void modoSSL(int puerto) throws IOException, KeyStoreException, FileNotFoundException,NoSuchAlgorithmException,CertificateException,UnrecoverableKeyException{
        //modo ssl del servidor
        String trustedStore="\\home\\oscar\\java\\jre1.8.0_371\\lib\\security\\serverTrustedStore.jks";
        String serverKey="\\home\\oscar\\java\\jre1.8.0_371\\lib\\security\\serverKey.jks";

        //acedemos al almacen de claves serverkey
        KeyStore store= KeyStore.getInstance("JKS");
        store.load(new FileInputStream(serverKey), "servpass".toCharArray());
        KeyManagerFactory Mfact=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        Mfact.init(store, "servpass".toCharArray());
        KeyManager[] keyManagers= Mfact.getKeyManagers();

        //acedemos al almacen de claves trustedstore
        KeyStore trusted= KeyStore.getInstance("JKS");
        trusted.load(new FileInputStream(trustedStore), "servpass".toCharArray());
        TrustManagerFactory tmFact=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFact.init(trusted);
        TrustManager[] trustManagers=tmFact.getTrustManagers();

        //intentamos conseguir sockets
        try{
            SSLContext socket= SSLContext.getInstance("SSL");
            socket.init(keyManagers,trustManagers,null);
            SSLServerSocketFactory socketFactory= socket.getServerSocketFactory();
            socketSSL = (SSLServerSocket) socketFactory.createServerSocket(puerto);
            socketSSL.setNeedClientAuth(true);

            System.out.println("Arrancado en modo SSL");

            //mientras que no se supere el numero  maximo de clientes 
            while(nCLientes<=clientesMax){
                SSLSocket socketC =(SSLSocket) socketSSL.accept();
                //AQUI EL METODO SERVIR PARA GESTIONAR TODAS LAS ACCIONES DEL CLIENTE
                nCLientes++;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void modoNormal(int puerto) throws IOException{
        ServerSocket s = new ServerSocket(puerto);
        System.out.println("servidor arrancado en modo normal.");

        //mientras que no se supere el numero max de clientes
        while(nCLientes<=clientesMax){
            Socket cliente=s.accept();
            //AQUI VA LA LLAMADA AL METODO SIRVE
            nCLientes++;
        }
    }
}
