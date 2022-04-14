package br.edu.utfpr.stratvision.utils;

import java.awt.Toolkit;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JOptionPane;

/**
 * @author Luis C. F. Bueno - 22/11/2021
 */
public class LogFile {
    
    public static final int NORMAL_MESSAGE = 0;
    public static final int ALERT_MESSAGE = 1;
    public static final int ERROR_MESSAGE   = 2;    
    
    private PrintStream outputFile;
    private File        file;
    private boolean     isFileCreated;
    
    private String                    filePrefix;
    private String                    systemName;
    private String                    folderName;    
    private GregorianCalendar         createdDate;    
    private boolean                   isDiary; // it is one file by day
    private LogFIleChangedListener listener;
    
    private static final SimpleDateFormat formataDataAtual = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss:SSS");
    
    public static LogFile createLogFile(String folder, String prefix, String systemName, boolean isDiary){
        String logFileName = mountNewLogFileName(folder,prefix);
        LogFile logFile = new LogFile(logFileName);
        logFile.registerOpening(systemName);
        logFile.folderName   = folder;
        logFile.filePrefix  = prefix;
        logFile.systemName     = systemName;
        logFile.isDiary = isDiary;
        return logFile;
    }    
        
    public static LogFile reopenLogFile(String completeName){
        LogFile logFile = new LogFile(completeName);
        logFile.registerReopening();
        return logFile;
    }
    
    public void createNewLogFile(){
        String logFileName = mountNewLogFileName(folderName,filePrefix);
        createsExternalFile(logFileName);
        registerOpening(systemName);
    }
    
    private LogFile(String logFileName)  {        
        listener = null;
        createsExternalFile(logFileName);
    }
    
    private static String mountNewLogFileName(String folder, String filePrefix){
        File folderFile = new File(folder);
        int     cont  = 0;        
        boolean folderExists = false;
        do{
            folderFile.mkdirs();
            folderExists = folderFile.exists();
            if (folderExists == false){
                cont++;
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException e){
                    
                }
            }
        }while(!folderExists && cont != 3);
        if (folderExists == false){                        
            String mensagem = "It wasn't possible to create the folder for the log file["+folder+ "]\n" +
                              "The application will be ended!";
            
            Toolkit.getDefaultToolkit().beep();
            System.err.println(mensagem);
            JOptionPane.showMessageDialog(null,mensagem,"Error warning",JOptionPane.ERROR_MESSAGE);        
            System.exit(-1);
        }
        Date      dataAtual      = new Date();
        SimpleDateFormat formato = new SimpleDateFormat("yyyy'-'MM'-'dd' Hora 'HH-mm-ss-SSS");
        try{
            folder = folderFile.getCanonicalPath();
        }catch (IOException e){
            String mensagem = "Error while trying recover the complete folder name created [" + folder +"]";
            Toolkit.getDefaultToolkit().beep();
            System.err.println(mensagem);
            JOptionPane.showMessageDialog(null,mensagem,"Error warning",JOptionPane.ERROR_MESSAGE);        
            System.exit(-1);
        }
        return folder + File.separator + filePrefix + formato.format(dataAtual) + ".log";
    }
    
    private void createsExternalFile(String logFileName){
        try{            
            file       = new File(logFileName);            
            outputFile  = new PrintStream(new FileOutputStream(file,true),true,"UTF-8");            
            createdDate   = new GregorianCalendar();
            isFileCreated = true;        
        }catch(FileNotFoundException | UnsupportedEncodingException erro){
            isFileCreated = false;         
            exceptionRegister(erro);            
        }     
    }
    
    private void checksCreationDateForNewFile(){
        if (isDiary){
            GregorianCalendar actual = new GregorianCalendar();
            if (actual.get(GregorianCalendar.DAY_OF_MONTH) != createdDate.get(GregorianCalendar.DAY_OF_MONTH)){
                registerFileEnding();
                closeFile();
                createsExternalFile(mountNewLogFileName(folderName,filePrefix));
                registerOpening(systemName);
                if (listener != null){
                    listener.logFileChanged(this);
                }
            }
        }
    }
    
    public File getFile(){
        return file;
    }
    
    public void addLogFileChangedListener(LogFIleChangedListener e){
        listener = e;
    }
    
    public void closeFile(){
        
         if (isFileCreated){
              isFileCreated = false;
              outputFile.close();              
         }
    }
    
    public boolean isOpen(){
        return isFileCreated;
    }
    
    public static String getActualDateTime(){     
        return formataDataAtual.format(new Date());         
    }
    
    public String getLogFullFileName(){
        
        if (isFileCreated){                    
            try{
                return file.getCanonicalPath();
            }catch(IOException ex){
                exceptionRegister(ex);
                return "Error triyng recorver full log file name";                
            }
        }else{
            return "Log file not created!";
        }
    }
    
    private void registerOpening(String systemName)  {
        
        if (isFileCreated){
            outputFile.println("###########################################################################");
            outputFile.println("####                     EVENTS LOG FILE REGISTER                      ####");
            outputFile.println("###########################################################################");                        
            outputFile.println("#### SYSTEM NAME       : ["  + systemName + "]");
            outputFile.println("###########################################################################");            
            outputFile.println("#### INITIAL LOG DATE : [" + getActualDateTime() + "]");
            outputFile.println("###########################################################################");            
        }
    }

    private void registerReopening()  {
        
        if (isFileCreated){            
            outputFile.println("###########################################################################");            
            outputFile.println("#### REOPEN LOG DATE : [" + getActualDateTime() + "]");
            outputFile.println("###########################################################################");            
        }
    }

    public synchronized void exceptionRegister(Exception error){        
        
        error.printStackTrace(System.err);
        System.err.println(error.getMessage());
        if (isFileCreated){
            checksCreationDateForNewFile();
            outputFile.println("[" + getActualDateTime() + "] -> #################################################");                    
            error.printStackTrace(outputFile);        
            outputFile.println("Error description [" + error.getLocalizedMessage() + "]\n");
            outputFile.println("###########################################################################");
        }
    }
    
    public synchronized void registerMessageWithoutCR(String msg){                
        
        if (isFileCreated){            
            
            checksCreationDateForNewFile();
            
            for (int x = 0; x < msg.length(); x++){                
                int caracter = msg.charAt(x);
                if (caracter == 10){                    
                    outputFile.println();
                }else{
                    outputFile.print(msg.charAt(x));
                }                
            }               
        }
    }
    
    public synchronized void registerMessage(String msg){                
        
        if (isFileCreated){            
            checksCreationDateForNewFile();
            for (int x = 0; x < msg.length(); x++){                
                int caracter = msg.charAt(x);
                if (caracter == 10){                    
                    outputFile.println();
                }else{
                    outputFile.print(msg.charAt(x));
                }                
            }   
            outputFile.println();
        }
    }
     
    public synchronized void registerMessageWithTime(String msg){        
        if (isFileCreated){           
            checksCreationDateForNewFile();
            msg = "[" + getActualDateTime() + "] -> "+ msg;           
            outputFile.println(msg);
        }
    }
    
    public synchronized void registerMessageWithTime(int type, String msg){
        if (isFileCreated){              
            checksCreationDateForNewFile();
            switch(type){
                case ERROR_MESSAGE:                    
                    msg = "[" + getActualDateTime() + "] -> "+ "#### [ERRO] ##### : " + msg;           
                    break;
                case ALERT_MESSAGE:                    
                    msg = "[" + getActualDateTime() + "] -> "+ "#### [ALERTA] ### : " + msg;           
                    break;
                default:
                    msg = "[" + getActualDateTime() + "] -> " + msg;           
                    break;
            }
            outputFile.println(msg);
        }        
    }
    
    public synchronized void registerSeparatorLine(){
        if (isFileCreated){
            outputFile.println("###########################################################################");
        }
    }
    
    public synchronized void registerFileEnding(){
        
        if (isFileCreated){
            outputFile.println("###########################################################################");
            outputFile.println("####  LOG FILE CLOSED - [ " + getActualDateTime() + "]");
            outputFile.println("###########################################################################");            
        }
    }
}