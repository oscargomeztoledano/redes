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
        String trustedStore="/home/oscar/java/jre1.8.0_371/lib/security/servertrustedstore.jks";
        String serverKey="/home/oscar/java/jre1.8.0_371/lib/security/serverkey.jks";

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
            sirve(cliente);
            nCLientes++;
        }
    }
    public void sirve(Socket cliente) {
        new Thread() {
            public void run() {
                try {

                    if (SSL) {
                        SSLSession s = ((SSLSocket) cliente).getSession();
                        System.out.println("Conectado cliente: " + s.getPeerHost() + ":" + s.getPeerPrincipal().toString());

                    } else {
                        System.out.println("Cliente (con puerto en" + cliente.getPort() + ") conectado desde "+ cliente.getInetAddress() + " al puerto " + cliente.getLocalPort());
                    }

                    String recibido;

                    // INPUT Y OUTPUT STREAMS
                    InputStream in = cliente.getInputStream();
                    OutputStream out = cliente.getOutputStream();

                    Scanner sc = new Scanner(in);
                    PrintWriter output = new PrintWriter(out);

                    recibido = sc.nextLine();

                    // LEER TODOS LOS ARCHIVOS DE UN DIRECTORIO
                    if (recibido.equals("LIST")) { // poner el else con que si no existe se hace el paso de error a la
                        acciones.info("Recibido comando LIST");							// carpeta log
                        String enviar = " ";
                        File fichero = new File(serverDir);
                        if (fichero.exists()) {// se comprueba si existe el el directorio
                        acciones.info("Ruta correcta");
                            File[] arrayFicheros = fichero.listFiles();
                            for (int i = 0; i < arrayFicheros.length; i++) {
                                enviar += "Archivo: " + arrayFicheros[i].getName() + " con tamano " + "("+ arrayFicheros[i].length() + ")" + "\n";
                            }
                        } else {
                        acciones.severe("No existe la carpeta");
                            out.write(("No existe la carpeta\n").getBytes());
                            out.flush();
                        }
                        output.println(enviar);
                    acciones.info("Listado enviado");
                        output.flush();

                        nCLientes--;
                        cliente.close();
                    }

                    // EL CLIENTE LE MANDA DOS COSAS AL SERVIDOR: Comando-Archivo
                    String lineas[] = recibido.split("\n");

                    if (lineas.length > 0) {
                        String tokens[] = lineas[0].split(" ");
                        if (tokens.length >= 2) {

                            // EL SERVIDOR DEVULEVE ARCHIVO PEDIDO AL CLIENTE
                            if (tokens[0].equals("GET")) {
                            acciones.info("Recibido comando GET");
                                try {
                                    String ruta = serverDir + "//" + tokens[1];
                                    File archivo = new File(ruta);

                                    if (archivo.exists() && !archivo.isDirectory()) {
                                    acciones.info("Ruta correcta");
                                        FileInputStream fis = new FileInputStream(archivo);
                                        BufferedInputStream bis = new BufferedInputStream(fis);
                                        byte[] buffer = new byte[(int) archivo.length()];
                                        bis.read(buffer, 0, buffer.length);

                                        OutputStream os = cliente.getOutputStream();
                                        os.write(buffer, 0, buffer.length);
                                        acciones.info("Archivo enviado");
                                        os.flush();

                                        bis.close();
                                        fis.close();

                                        cliente.close();
                                        sc.close();
                                        nCLientes--;
                                    } else {
                                        // Archivo no encontrado
                                        errores.severe("No se ha encontrado el archivo");
                                        String respuesta = "NO se ha encontrado el archivo";
                                        output.println(respuesta);
                                        output.flush();
                                        cliente.close();
                                        sc.close();
                                        nCLientes--;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            // INTRODUCIR UN FICHERO MANDADO POR EL CLIENTE
                            else if (tokens[0].equals("PUT")) {
                            acciones.info("Recibido comando PUT");
                                try {
                                    String ruta = serverDir + "//" + tokens[1];
                                    FileOutputStream fos = new FileOutputStream(ruta);

                                    byte[] buffer = new byte[1024];
                                    int bytesRead;
                                    while ((bytesRead = cliente.getInputStream().read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytesRead);
                                    }
                                    fos.close();
                                    acciones.info("Archivo almacenado correctamente");
                                    cliente.close();
                                    nCLientes--;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }


                        }
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }.start();
    }
    }