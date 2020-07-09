import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.concurrent.*;

public class JSchExampleSSHConnectionWithThreads {
    public static void main(String[] args) {
       long startTime =  System.currentTimeMillis();
        Session session = openSshSession();
        for(int i  = 0 ; i < 100; i++) {
            System.out.println(i);
            if(i == 10){
                execCommandInSession(session);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Duration using connection on a thread: " + (endTime - startTime));
    }

    private static Session openSshSession() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Future<Session> sessionFuture = executorService.submit(sessionCallable());
        try {
            executorService.shutdown();
            return sessionFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error in opening connection");
    }

    public static Callable<Session> sessionCallable(){
       Callable<Session> callable = () ->{
           String host = "test.rebex.net";
           String user = "demo";
           String password = "password";
           try {
               java.util.Properties config = new java.util.Properties();
               config.put("StrictHostKeyChecking", "no");
               JSch jsch = new JSch();
               Session session = jsch.getSession(user, host, 22);
               session.setPassword(password);
               session.setConfig(config);
               session.connect();
               System.out.println("Connected");
               return session;
           } catch (Exception e) {
               e.printStackTrace();
           }
           throw new RuntimeException("Could not open ssh connection");
       };
       return callable;
    }


    private static void execCommandInSession(Session session) {
        String command1 = "ls -ltr";
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command1);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
            }
            channel.disconnect();
            session.disconnect();
            System.out.println("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
