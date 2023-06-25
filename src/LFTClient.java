import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
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
        File file = new File("logErrores.txt");    //creamos los archivos de registro
        File file2= new File("logAcciones.txt");
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
    }
    LFTClient(String host,int puerto) throws Exception{
        //modo ssl del servidor
        String clientStore="\\home\\oscar\\java\\jre1.8.0_371\\lib\\security\\clientKey.jks";
        String cacerts="\\home\\oscar\\java\\jre1.8.0_371\\lib\\security\\cacerts";

        //acedemos al almacen de claves serverkey
        KeyStore store= KeyStore.getInstance("JKS");
        store.load(new FileInputStream(clientStore), "clientpass".toCharArray());
        KeyManagerFactory Mfact=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        Mfact.init(store, "clientpass".toCharArray());
        KeyManager[] keyManagers= Mfact.getKeyManagers();

        //acedemos al almacen de claves trustedstore
        KeyStore trusted= KeyStore.getInstance("JKS");
        trusted.load(new FileInputStream(cacerts), "servpass".toCharArray());
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
 
}