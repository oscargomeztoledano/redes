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
    private static int nCLientes;
    private SSLServerSocket socketSSL;
    


    public static void main(String[] args) throws UnrecoverableKeyException,CertificateException,NoSuchAlgorithmException,IOException,KeyStoreException{
        
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

        LFTServer server= new LFTServer();
        //dependiendo de el modo seleccionado se inicia el server ssl o el normal 


        //CONTRASEÑA password
        

    }
}
